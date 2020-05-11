FROM phusion/baseimage

MAINTAINER Jakub Balhar

#Install necessary dependencies
RUN install_clean build-essential wget software-properties-common openjdk-8-jdk git curl python3 python3-pip python3-dev \
        wget libgtk2.0-0 libgtk-3-0 libnotify-dev libgconf-2-4 libnss3 libxss1 libasound2 libxtst6 xauth xvfb

# Install Node related dependencies
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.35.3/install.sh | bash \
    && export NVM_DIR="$HOME/.nvm" \
    && [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"  \
    && [ -s "$NVM_DIR/bash_completion" ] && \. "$NVM_DIR/bash_completion"  \
    && nvm install v12.16.1 \
    && npm add -g pnpm \
    && npm install -g concurrently

# Prepare environment
RUN mkdir /var/src

# Install Zowe-CLI
RUN export NVM_DIR="$HOME/.nvm" \
    && [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"  \
    && [ -s "$NVM_DIR/bash_completion" ] && \. "$NVM_DIR/bash_completion"  \
    && nvm use v12.16.1 \
    && npm install -g @zowe/cli --ignore-scripts \
    && npm -g install @zowedev/zowe-api-dev

# Clean up APT when done.
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Download public api-layer
RUN cd /var/src \
    && git clone https://github.com/zowe/api-layer.git \
    && cd /var/src/api-layer \
    && export NVM_DIR="$HOME/.nvm" \
    && [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"  \
    && [ -s "$NVM_DIR/bash_completion" ] && \. "$NVM_DIR/bash_completion"  \
    && nvm use v12.16.1 \
    && cd /var/src/api-layer/api-catalog-ui/frontend/; pnpm install; cd ../.. \
    && ./gradlew clean build

# Copy all the remaining scripts
COPY ./_* /bin/

# Other.
RUN export TERM=xterm

ENTRYPOINT ["/sbin/my_init"]
