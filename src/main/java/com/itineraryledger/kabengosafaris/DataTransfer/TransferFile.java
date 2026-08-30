package com.itineraryledger.kabengosafaris.DataTransfer;

/**
 * One file a module wants carried in the bundle, and where it belongs on the other side.
 *
 * `path` is the name INSIDE the bundle, not on disk. Two companies store their images in different
 * directories under different service accounts, so a bundle that carried absolute paths would be a
 * bundle that only imports back into the machine it came from.
 */
public record TransferFile(String path, java.nio.file.Path source) {}
