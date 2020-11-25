FROM docker-private.packages.nuxeo.com/nuxeo/nuxeo:11.4.38

COPY --chown=nuxeo:0 packages/google-storage-*.zip /home/nuxeo/local-packages/google-storage.zip
COPY --chown=nuxeo:0 packages/nuxeo-web-ui-*.zip /home/nuxeo/local-packages/nuxeo-web-ui.zip
COPY --chown=nuxeo:0 plugin/plugin-package/target/plugin-package*.zip /home/nuxeo/local-packages/plugin-package.zip

# Work around missing support for --chown flag with COPY instruction in Kaniko
# TODO NXP-28052: remove when fixed in Kaniko, or find a proper way
USER root
RUN /install-packages.sh /home/nuxeo/local-packages/google-storage.zip /home/nuxeo/local-packages/nuxeo-web-ui.zip /home/nuxeo/local-packages/plugin-package.zip
RUN chown -R 900:0 ${NUXEO_HOME} \
  && chmod -R g+rwX ${NUXEO_HOME}
USER 900
