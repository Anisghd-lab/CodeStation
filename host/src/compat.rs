//! Phone/host compatibility: the host names in `hello` the oldest app version
//! (`MIN_APP`) it still serves correctly; the Android app keeps its own floor.

/// The oldest app this host still serves correctly. Raise it only when this host
/// stops sending or accepting something older apps depend on.
pub const MIN_APP: &str = "2.21.0";
