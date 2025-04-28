# Copyright 2021 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Userservice manages user account creation, user login, and related tasks
"""

import atexit
from datetime import datetime, timedelta
import logging
import os
import sys
import re
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

import bcrypt
import jwt
from flask import Flask, jsonify, request, abort
import bleach
from sqlalchemy.exc import OperationalError, SQLAlchemyError

from opentelemetry import trace
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.propagate import set_global_textmap
from opentelemetry.exporter.cloud_trace import CloudTraceSpanExporter
from opentelemetry.propagators.cloud_trace_propagator import CloudTraceFormatPropagator
from opentelemetry.instrumentation.flask import FlaskInstrumentor

from db import UserDb

def create_app():
    """Flask application factory to create instances
    of the Userservice Flask App
    """
    app = Flask(__name__)

    # Disabling unused-variable for lines with route decorated functions
    # as pylint thinks they are unused
    # pylint: disable=unused-variable


    def verify_token(token):
        """
        Validates token using userservice public key
        """
        app.logger.debug('Verifying token.')
        if token is None:
            return False
        try:
            jwt.decode(algorithms='RS256',
                       jwt=token,
                       key=app.config['PUBLIC_KEY'],
                       options={"verify_signature": True})
            app.logger.debug('Token verified.')
            return True
        except jwt.exceptions.InvalidTokenError as err:
            app.logger.error('Error validating token: %s', str(err))
            return False
        
    def send_welcome_email(username, email, firstname):
        """
        Send a welcome email to a newly registered user
        
        Args:
            username: User's username
            email: User's email address
            firstname: User's first name
        """
        try:
            # Get email configuration from environment variables
            mail_server = os.environ.get('MAIL_SERVER')
            mail_port = int(os.environ.get('MAIL_PORT', '587'))
            mail_sender = os.environ.get('MAIL_DEFAULT_SENDER')
            mail_password = os.environ.get('EMAIL_PASSWORD')
            use_tls = os.environ.get('MAIL_USE_TLS', 'false').lower() == 'true'
            
            # Create message
            msg = MIMEMultipart()
            msg['From'] = f"Group 3 Banking Team <{mail_sender}>"
            msg['To'] = email
            msg['Subject'] = f"Welcome to Our Group 3 Banking, {firstname}!"
            
            # Create styled email body with CSS
            body = f"""
            <html>
            <head>
                <style>
                    body {{
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333333;
                    }}
                    .container {{
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                        border: 1px solid #dddddd;
                        border-radius: 5px;
                    }}
                    .header {{
                        background-color: #4285f4;
                        color: white;
                        padding: 15px;
                        text-align: center;
                        border-radius: 5px 5px 0 0;
                    }}
                    .content {{
                        padding: 20px;
                    }}
                    .footer {{
                        background-color: #f5f5f5;
                        padding: 10px;
                        text-align: center;
                        font-size: 12px;
                        border-radius: 0 0 5px 5px;
                    }}
                    .highlight {{
                        font-weight: bold;
                        color: #4285f4;
                    }}
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>Welcome to Our Group 3 Banking Service!</h2>
                    </div>
                    <div class="content">
                        <p>Hello <span class="highlight">{firstname}</span>,</p>
                        
                        <p>Thank you for registering with our service. Your account has been successfully created and is now ready to use.</p>
                        
                        <p>Your username: <span class="highlight">{username}</span></p>
                        
                        <p>You can now log in using your credentials to access all our features and services.</p>
                        
                        <p>If you have any questions or need assistance, please don't hesitate to contact our support team.</p>
                        
                        <p>Best regards,<br>The Service Team</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message. Please do not reply to this email.</p>
                        <p>&copy; 2025 Our Service. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """
            
            # Attach the body to the message
            msg.attach(MIMEText(body, 'html'))
            
            # Connect to the SMTP server
            server = smtplib.SMTP(mail_server, mail_port)
            if use_tls:
                server.starttls()
            
            # Login and send email
            server.login(mail_sender, mail_password)
            server.send_message(msg)
            server.quit()
            
            app.logger.info(f"Welcome email sent successfully to {email}")
            return True
        except Exception as e:
            app.logger.error(f"Failed to send welcome email: {str(e)}")
            return False

    @app.route('/version', methods=['GET'])
    def version():
        """
        Service version endpoint
        """
        return app.config['VERSION'], 200

    @app.route('/ready', methods=['GET'])
    def readiness():
        """
        Readiness probe
        """
        return 'ok', 200



    @app.route('/users', methods=['POST'])
    def create_user():
        """Create a user record.

        Fails if that email or username already exists.

        Generates a unique accountid.

        request fields:
        - email
        - username
        - password
        - password-repeat
        - firstname
        - lastname
        - birthday
        - timezone
        - address
        - state
        - zip
        - ssn
        """
        try:
            app.logger.debug('Sanitizing input.')
            req = {k: bleach.clean(v) for k, v in request.form.items()}
            __validate_new_user(req)

            # Check if email already exists
            if users_db.email_exists(req['email']):
                raise NameError('user with email {} already exists'.format(req['email']))

            # Check if user already exists
            if users_db.get_user(req['username']) is not None:
                raise NameError('user {} already exists'.format(req['username']))

            # Create password hash with salt
            app.logger.debug("Creating password hash.")
            password = req['password']
            salt = bcrypt.gensalt()
            passhash = bcrypt.hashpw(password.encode('utf-8'), salt)

            accountid = users_db.generate_accountid()

            # Create user data to be added to the database
            user_data = {
                'accountid': accountid,
                'username': req['username'],
                'passhash': passhash,
                'firstname': req['firstname'],
                'lastname': req['lastname'],
                'birthday': req['birthday'],
                'timezone': req['timezone'],
                'address': req['address'],
                'state': req['state'],
                'zip': req['zip'],
                'ssn': req['ssn'],
                'email': req['email'],  # Add email field
            }
            # Add user_data to database
            app.logger.debug("Adding user to the database")
            users_db.add_user(user_data)
            app.logger.info("Successfully created user.")

            # Send welcome email
            app.logger.debug("Sending welcome email")
            email_sent = send_welcome_email(
                username=req['username'],
                email=req['email'],
                firstname=req['firstname']
            )
            if not email_sent:
                app.logger.warning(f"Failed to send welcome email to {req['email']}")

        except UserWarning as warn:
            app.logger.error("Error creating new user: %s", str(warn))
            return str(warn), 400
        except NameError as err:
            app.logger.error("Error creating new user: %s", str(err))
            return str(err), 409
        except SQLAlchemyError as err:
            app.logger.error("Error creating new user: %s", str(err))
            return 'failed to create user', 500

        return jsonify({}), 201

    def __validate_new_user(req):
        app.logger.debug('validating create user request: %s', str(req))
        # Check if required fields are filled
        fields = (
            'email',
            'username',
            'password',
            'password-repeat',
            'firstname',
            'lastname',
            'birthday',
            'timezone',
            'address',
            'state',
            'zip',
            'ssn',
        )
        if any(f not in req for f in fields):
            raise UserWarning('missing required field(s)')
        if any(not bool(req[f] or req[f].strip()) for f in fields):
            raise UserWarning('missing value for input field(s)')

        # Verify email format
        if not re.match(r"[^@]+@[^@]+\.[^@]+", req['email']):
            raise UserWarning('invalid email format')

        # Verify username contains only 2-15 alphanumeric or underscore characters
        if not re.match(r"\A[a-zA-Z0-9_]{2,15}\Z", req['username']):
            raise UserWarning('username must contain 2-15 alphanumeric characters or underscores')
        # Check if passwords match
        if not req['password'] == req['password-repeat']:
            raise UserWarning('passwords do not match')

    @app.route('/login', methods=['GET'])
    def login():
        """Login a user and return a JWT token

        Fails if email doesn't exist or password doesn't match hash

        token expiry time determined by environment variable

        request fields:
        - username
        - password
        """
        app.logger.debug('Sanitizing login input.')
        username = bleach.clean(request.args.get('username'))
        password = bleach.clean(request.args.get('password'))

        # Get user data
        try:
            app.logger.debug('Getting the user data.')
            user = users_db.get_user(username)
            if user is None:
                raise LookupError('user {} does not exist'.format(username))

            # Validate the password
            app.logger.debug('Validating the password.')
            if not bcrypt.checkpw(password.encode('utf-8'), user['passhash']):
                raise PermissionError('invalid login')

            full_name = '{} {}'.format(user['firstname'], user['lastname'])
            exp_time = datetime.utcnow() + timedelta(seconds=app.config['EXPIRY_SECONDS'])
            payload = {
                'user': username,
                'acct': user['accountid'],
                'name': full_name,
                'iat': datetime.utcnow(),
                'exp': exp_time,
            }
            app.logger.debug('Creating jwt token.')
            token = jwt.encode(payload, app.config['PRIVATE_KEY'], algorithm='RS256')
            app.logger.info('Login Successful.')
            return jsonify({'token': token}), 200

        except LookupError as err:
            app.logger.error('Error logging in: %s', str(err))
            return str(err), 404
        except PermissionError as err:
            app.logger.error('Error logging in: %s', str(err))
            return str(err), 401
        except SQLAlchemyError as err:
            app.logger.error('Error logging in: %s', str(err))
            return 'failed to retrieve user information', 500

    @app.route('/users/<username>', methods=['GET'])
    def get_user(username):
        """
        Get user details
        
        Returns user details for the specified username if authenticated
        """
        if not verify_token(request.headers.get('Authorization')):
            return abort(401)
        
        try:
            # Get user from database
            user = users_db.get_user(username)
            if user is None:
                return jsonify({'error': 'User not found'}), 404
                
            # Return only necessary user data
            user_data = {
                'username': user['username'],
                'email': user['email'],
                'firstname': user['firstname'],
                'lastname': user['lastname']
            }
            
            return jsonify(user_data), 200
            
        except Exception as e:
            app.logger.error('Error retrieving user data: %s', str(e))
            return jsonify({'error': 'Failed to retrieve user data'}), 500

    @atexit.register
    def _shutdown():
        """Executed when web app is terminated."""
        app.logger.info("Stopping userservice.")

    # Set up logger
    app.logger.handlers = logging.getLogger('gunicorn.error').handlers
    app.logger.setLevel(logging.getLogger('gunicorn.error').level)
    app.logger.info('Starting userservice.')

    # Set up tracing and export spans to Cloud Trace.
    if os.environ['ENABLE_TRACING'] == "true":
        app.logger.info("✅ Tracing enabled.")
        # Set up tracing and export spans to Cloud Trace
        trace.set_tracer_provider(TracerProvider())
        cloud_trace_exporter = CloudTraceSpanExporter()
        trace.get_tracer_provider().add_span_processor(
            BatchSpanProcessor(cloud_trace_exporter)
        )
        set_global_textmap(CloudTraceFormatPropagator())
        FlaskInstrumentor().instrument_app(app)
    else:
        app.logger.info("🚫 Tracing disabled.")

    app.config['VERSION'] = os.environ.get('VERSION')
    app.config['EXPIRY_SECONDS'] = int(os.environ.get('TOKEN_EXPIRY_SECONDS'))
    app.config['PRIVATE_KEY'] = open(os.environ.get('PRIV_KEY_PATH'), 'r').read()
    app.config['PUBLIC_KEY'] = open(os.environ.get('PUB_KEY_PATH'), 'r').read()

    # Configure database connection
    try:
        users_db = UserDb(os.environ.get("ACCOUNTS_DB_URI"), app.logger)
    except OperationalError:
        app.logger.critical("users_db database connection failed")
        sys.exit(1)
    return app


if __name__ == "__main__":
    # Create an instance of flask server when called directly
    USERSERVICE = create_app()
    USERSERVICE.run()
