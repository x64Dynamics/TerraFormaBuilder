<div align="center">

| [📖<br>Overview](README.md) | [📦<br>Releases](RELEASES.md) | [🤝<br>Contributing](CONTRIBUTING.md) | [🛡️<br>Security](SECURITY.md) | [⚖️<br><nobr>Code of Conduct</nobr>](CODE_OF_CONDUCT.md) |
| :---: | :---: | :---: | :---: | :---: |

</div>

---

# Contributing to TerraFormaBuilder

Thank you for your interest in contributing to **TerraFormaBuilder**! TFB is developed by **x64Dynamics** with lead developer **KyroXova**.

TFB is an open-source, multi-loader architectural construction engine designed for pure Java mathematical foundations with unencumbered cross-version adapters supporting Fabric, Forge, and NeoForge across 30 Minecraft releases (1.18.2 through 26.3).

---

## Code of Conduct

All contributors and community participants are expected to uphold our [Code of Conduct](CODE_OF_CONDUCT.md) to ensure an open, welcoming, and harassment-free environment for everyone.

---

## Reporting Issues

Before opening a new issue, search existing issues to verify that the problem or suggestion has not already been tracked.

* **Bug Reports**: Please use the [Bug Report Template](.github/ISSUE_TEMPLATE/bug.md). Provide the exact Minecraft release, mod loader, mod version, Java runtime, full client/server logs (`logs/latest.log`), and step-by-step reproduction instructions.
* **Feature Suggestions**: Use the [Feature Suggestion Template](.github/ISSUE_TEMPLATE/suggestion.md) to detail proposed architectural shapes, infrastructure tools, brushes, or algorithmic enhancements.
* **Questions & Inquiries**: Use the [Question Template](.github/ISSUE_TEMPLATE/question.md).

---

## Development Setup

TerraFormaBuilder uses an unencumbered multi-target Gradle and Python matrix architecture.

### Prerequisites

* **Git**
* **JDK 17, 21, and 25** installed and configured in your environment
* **Python 3.8+** (for matrix orchestration and validation tools)

### Validating Target Configurations

Run the central structural and isolation validator:
```bash
python tools/kyro/template.py validate
```

### Compiling Core Logic & Unit Tests

To compile and test pure geometric, mathematical, taxonomy, and UI state models:
```bash
python tools/kyro/template.py core
```

### Compiling & Running Locally

Use the root launcher script to build or test individual target releases:
```bash
# Build specific release targets
.\modw.bat -Pmc=1.20.1 -Ploader=fabric build
.\modw.bat -Pmc=1.20.1 -Ploader=forge build
.\modw.bat -Pmc=1.21.1 -Ploader=neoforge build

# Launch the Minecraft test client
.\modw.bat -Pmc=1.21.1 -Ploader=neoforge runClient
```

---

## Pull Request Guidelines

1. **Branching**:
   - Create a clean feature branch from `master` or the latest active development branch.
   - Do not open PRs directly targeting production without verifying cross-loader parity.
2. **Architecture & Source Isolation**:
   - Keep all pure procedural geometry, taxonomy, models, and UI logic in `common/`.
   - Never import Minecraft or loader classes inside `common/src/main/java`.
   - Implement loader-specific entrypoints and event bindings inside `versions/<version>/<loader>/`.
3. **Commit Messages & Authorship**:
   - Write clear, imperative, professional commit messages.
   - Do not include AI attribution tags, bot trailers (`Co-Authored-By`, `Generated with`), or model references in commits.
4. **Author & Studio Attribution**:
   - TerraFormaBuilder is maintained by **x64Dynamics** with lead developer **KyroXova**.
5. **Testing & Parity**:
   - Verify that your additions maintain universal feature parity across Fabric, Forge, and NeoForge.
   - Ensure `python tools/kyro/template.py validate` and `python tools/kyro/template.py core` pass before submitting.
