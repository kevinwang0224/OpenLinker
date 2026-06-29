# OpenLinker Changelog

## [Unreleased]

## [0.1.4] - 2026-06-29

### Added

- Added project-specific URL overrides for individual rules.
- Added import/export support for project-specific rule overrides in the same JSON rule file.

### Changed

- Rule choosers and the settings table now show the effective project-specific URL when the current project has an override.
- The OpenLinker settings page now receives the current project context while keeping rules in the shared OpenLinker settings file.

## [0.1.3] - 2026-04-25

### Added

- Added browser selection support in OpenLinker settings.
- Added a global browser setting and per-rule browser overrides.

### Changed

- Rule browser choices now stay personal and are not included in rule import/export files.
- Rules can now be enabled or disabled directly from the settings table.

### Fixed

- Fixed the `Tools > OpenLinker` popup position so it opens centered in the current window.
