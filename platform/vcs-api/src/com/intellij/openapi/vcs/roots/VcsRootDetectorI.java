package com.intellij.openapi.vcs.roots;

import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Interface for detecting VCS roots in the project.
 *
 * @author Nadya Zabrodina
 */
public interface VcsRootDetectorI {

  /**
   * Detect vcs roots for whole project
   */
  @NotNull
  Collection<VcsRoot> detect();

  /**
   * Detect vcs roots for startDir
   */
  @NotNull
  Collection<VcsRoot> detect(@Nullable VirtualFile startDir);
}
