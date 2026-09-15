#!/bin/sh
python manage.py collectstatic --noinput
python manage.py migrate --noinput
python manage.py createsuperuser --noinput || true
exec gunicorn --bind 0.0.0.0:8080 azshserver.wsgi:application