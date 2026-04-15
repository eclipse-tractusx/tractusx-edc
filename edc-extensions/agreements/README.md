## Purpose
The Agreements extension allows a **data provider** to **prematurely retire** an active contract agreement. Contract agreements are immutable in EDC; they normally expire only when the contractual terms no longer hold. This extension addresses cases where the digital agreement must be invalidated earlier—for example when a physical or legal agreement changes and the digital representation should no longer allow data transfers.

Retirement is implemented by storing retirement entries and failing policy validation when a transfer or policy-monitor flow uses a retired agreement. The extension provides a Management API to manage retirement entries and optional SQL persistence.
