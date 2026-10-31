# OpenWiki Setup State

## Local CLI Status

Requested local install command:

```powershell
npm install -g openwiki
```

Observed result in this shell:

```text
npm: The term 'npm' is not recognized as a name of a cmdlet, function, script file, or executable program.
```

Requested initialization command:

```powershell
openwiki --init
```

Expected current result until installation is fixed:

```text
openwiki: The term 'openwiki' is not recognized as a name of a cmdlet, function, script file, or executable program.
```

System `node` and `npm` were not found on `PATH`. Codex's bundled runtime has Node.js 24.14.0, but this bundled runtime did not expose an npm command.

## Minimal Local Install Requirement

Install Node.js 22 LTS or newer from the official Node.js installer so that both `node` and `npm` are available in PowerShell, then run:

```powershell
npm install -g openwiki
openwiki --init
```

## Provider Secrets

No provider/model environment variables were present in this shell:

- `OPENROUTER_API_KEY`
- `OPENAI_API_KEY`
- `ANTHROPIC_API_KEY`
- `OPENWIKI_MODEL_ID`
- `LANGSMITH_API_KEY`

Set one provider API key before running OpenWiki:

- OpenRouter: `OPENROUTER_API_KEY`
- OpenAI: `OPENAI_API_KEY`
- Anthropic: `ANTHROPIC_API_KEY`

Also set `OPENWIKI_MODEL_ID` when you want a specific model. Use `LANGSMITH_API_KEY` only if tracing is intentionally configured.

Do not commit local API keys. OpenWiki stores local configuration under `~/.openwiki/.env` after interactive setup.

## GitHub Action Secrets

The repository workflow at `.github/workflows/openwiki-update.yml` expects provider API keys from repository secrets, not hardcoded values. Configure at least one of:

- `OPENROUTER_API_KEY`
- `OPENAI_API_KEY`
- `ANTHROPIC_API_KEY`

Configure `OPENWIKI_MODEL_ID` as a repository variable or secret if the default provider setup is not sufficient.
