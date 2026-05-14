#!/usr/bin/env bash
set -euo pipefail

mkdir -p certs/dev
cd certs/dev

openssl genrsa -out ca.key 4096
openssl req -x509 -new -nodes \
  -key ca.key \
  -sha256 \
  -days 3650 \
  -subj "/CN=dev-root-ca" \
  -out ca.crt

cat > server.ext <<EOF
subjectAltName=DNS:localhost,IP:127.0.0.1
extendedKeyUsage=serverAuth
keyUsage=digitalSignature,keyEncipherment
EOF

openssl req -newkey rsa:2048 -nodes \
  -keyout server.key \
  -subj "/CN=grpc-server" \
  -out server.csr

openssl x509 -req \
  -in server.csr \
  -CA ca.crt \
  -CAkey ca.key \
  -CAcreateserial \
  -out server.crt \
  -days 365 \
  -sha256 \
  -extfile server.ext

cat > service-a.ext <<EOF
subjectAltName=URI:spiffe://internal/ns/default/sa/service-a
extendedKeyUsage=clientAuth
keyUsage=digitalSignature
EOF

openssl req -newkey rsa:2048 -nodes \
  -keyout service-a.key \
  -subj "/CN=service-a" \
  -out service-a.csr

openssl x509 -req \
  -in service-a.csr \
  -CA ca.crt \
  -CAkey ca.key \
  -CAcreateserial \
  -out service-a.crt \
  -days 365 \
  -sha256 \
  -extfile service-a.ext

cat > service-b.ext <<EOF
subjectAltName=URI:spiffe://internal/ns/default/sa/service-b
extendedKeyUsage=clientAuth
keyUsage=digitalSignature
EOF

openssl req -newkey rsa:2048 -nodes \
  -keyout service-b.key \
  -subj "/CN=service-b" \
  -out service-b.csr

openssl x509 -req \
  -in service-b.csr \
  -CA ca.crt \
  -CAkey ca.key \
  -CAcreateserial \
  -out service-b.crt \
  -days 365 \
  -sha256 \
  -extfile service-b.ext
