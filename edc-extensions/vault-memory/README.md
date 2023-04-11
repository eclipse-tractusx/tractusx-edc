# In-memory Vault implementation

The goal of this extension is to provide an ephemeral, memory-based vault implementation that can be used in testing or
demo scenarios.

Please not that this vault does not encrypt the secrets, they are held in memory in plain text at runtime! In addition,
its ephemeral nature makes it unsuitable for replicated/multi-instance scenarios, i.e. Kubernetes.

> It is not a secure secret store, please do NOT use it in production workloads!
