# Contributing Guide

Thanks for your interest in contributing to this project.

## Development Setup

1. Fork this repository and create a feature branch.
2. Start backend:

```bash
cd backend
mvn spring-boot:run
```

3. Start frontend:

```bash
cd frontend
npm install
npm run dev
```

## Code Style

- Keep changes focused and small.
- Do not mix refactor and feature logic in one PR.
- Preserve existing API behavior unless explicitly discussed.
- Use TypeScript strict mode friendly patterns in frontend code.

## Commit and PR

1. Use clear commit messages.
2. In PR description, include:
   - problem background
   - core changes
   - test/verification steps
   - screenshots or request examples if UI/API changed
3. Link related issues when possible.

## Validation Checklist

Before opening PR, ensure:

- Frontend builds successfully:

```bash
cd frontend
npm run build
```

- Backend compiles successfully:

```bash
cd backend
mvn -DskipTests compile
```

## Reporting Issues

When submitting an issue, include:

- steps to reproduce
- expected behavior
- actual behavior
- environment details (OS, Node, Java)

Thank you for helping improve this project.
