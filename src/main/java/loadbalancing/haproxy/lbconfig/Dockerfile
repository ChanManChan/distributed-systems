FROM haproxy:1.7
COPY haproxy_tcp_mode.cfg /usr/local/etc/haproxy/haproxy_tcp_mode.cfg
ENTRYPOINT ["haproxy", "-f", "/usr/local/etc/haproxy/haproxy_tcp_mode.cfg"]