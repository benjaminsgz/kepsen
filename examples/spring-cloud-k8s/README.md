# Spring Cloud Kubernetes Example

This example shows how to run a Spring Cloud gRPC service with Kepsen in Kubernetes.

The deployment model is intentionally file-based:

- TLS material is mounted from a Kubernetes `Secret`.
- Spring/Kepsen configuration is mounted from a `ConfigMap`.
- The application reads `/etc/kepsen/config/application.yml` at startup.

This keeps private keys out of environment variables and avoids giving the application Kubernetes API permissions just to read its own configuration.

## Files

| File | Purpose |
|---|---|
| `secret.yaml` | Example mTLS server certificate, key, and trusted client CA bundle. Replace all PEM values before use. |
| `configmap.yaml` | Spring Boot, gRPC, mTLS, and method ACL configuration. |
| `deployment.yaml` | Mounts the Secret and ConfigMap into the application Pod. |
| `service.yaml` | Exposes the gRPC port inside the cluster. |

## Apply

```bash
kubectl apply -f secret.yaml
kubectl apply -f configmap.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
```

## Production Notes

- Use `service-acl.default-action: deny`.
- Prefer `service-acl.identity-source: san-uri`; CN fallback should be treated as legacy or development-only.
- Rotate certificates by updating the Secret and rolling the Deployment. Kepsen builds the Netty TLS context at startup, so mounted file updates alone do not replace the live TLS context.
- If a service mesh or ingress terminates TLS before traffic reaches this Pod, Kepsen cannot read the original client certificate from the gRPC `SSLSession`.
