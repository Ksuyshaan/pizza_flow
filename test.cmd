@echo off
rem Uses mvnw-local.cmd (personal, not in git) when present, otherwise the standard Maven Wrapper.
if exist "%~dp0mvnw-local.cmd" (
    call "%~dp0mvnw-local.cmd" test %*
) else (
    call "%~dp0mvnw.cmd" test %*
)
