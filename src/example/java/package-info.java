/// Example domain showcasing the JavaDoc constructs that jMarkDoc renders.
///
/// This package exists purely to demonstrate the generator end to end. It is a
/// minimal account-management slice built around [UserService] and its nested
/// [UserService.User] record, written so the produced Markdown exercises every
/// supported section.
///
/// ## Boundary
/// - `UserService` — create, look up, and deactivate accounts
/// - `UserService.User` — the immutable account record
/// - `UserService.NotFoundException` — signals a missing account
///
/// The comments here are authored as Markdown — headings, lists, inline `code`,
/// and the em dash (—) all pass through to the generated output verbatim.
package airhacks.example;
