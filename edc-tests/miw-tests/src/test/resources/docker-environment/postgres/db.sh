#
# Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0
#
# SPDX-License-Identifier: Apache-2.0
#
# Contributors:
#       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
#
#

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE $POSTGRES_DB_NAME_MIW;
    CREATE USER $POSTGRES_USERNAME_MIW WITH ENCRYPTED PASSWORD '$POSTGRES_PASSWORD_MIW';
    GRANT ALL PRIVILEGES ON DATABASE $POSTGRES_DB_NAME_MIW TO $POSTGRES_USERNAME_MIW;
    \c $POSTGRES_DB_NAME_MIW
    GRANT ALL ON SCHEMA public TO $POSTGRES_USERNAME_MIW;
EOSQL