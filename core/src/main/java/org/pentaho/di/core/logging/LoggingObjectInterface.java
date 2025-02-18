/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.core.logging;

import java.util.Date;

import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.ObjectRevision;
import org.pentaho.di.repository.RepositoryDirectoryInterface;

public interface LoggingObjectInterface extends LoggingObjectLifecycleInterface {
  /**
   * Gets the object name.
   *
   * @return the name
   */
  public String getObjectName();

  /**
   * Gets the repository directory.
   *
   * @return the repositoryDirectory
   */
  public RepositoryDirectoryInterface getRepositoryDirectory();

  /**
   * Gets the filename.
   *
   * @return the filename
   */
  public String getFilename();

  /**
   * Gets the object id in a repository.
   *
   * @return the objectId in a repository
   */
  public ObjectId getObjectId();

  /**
   * Gets the objects revision in a repository.
   *
   * @return the object revision in a repository
   */
  public ObjectRevision getObjectRevision();

  /**
   * Gets the log channel id.
   *
   * @return the log channel id
   */
  public String getLogChannelId();

  /**
   * Gets the parent.
   *
   * @return the parent
   */
  public LoggingObjectInterface getParent();

  /**
   * Gets the object type.
   *
   * @return the objectType
   */
  public LoggingObjectType getObjectType();

  /**
   * Gets a string identifying a copy in a series of steps.
   *
   * @return A string identifying a copy in a series of steps.
   */
  public String getObjectCopy();

  /**
   * Gets the logging level of the log channel of this logging object.
   *
   * @return The logging level of the log channel of this logging object.
   */
  public LogLevel getLogLevel();

  /**
   * Gets the execution container (Carte/DI server/BI Server) object id. We use this to see to which copy of the
   * job/trans hierarchy this object belongs. If it is null, we assume that we are running a single copy in
   * Spoon/Pan/Kitchen.
   *
   * @return The execution container (Carte/DI server/BI Server) object id.
   */
  public String getContainerObjectId();

  /**
   * Gets the registration date of this logging object. Null if it's not registered.
   *
   * @return The registration date of this logging object. Null if it's not registered.
   */
  public Date getRegistrationDate();

  /**
   * Gets the boolean value of whether or not this object is gathering kettle metrics during execution.
   *
   * @return true if this logging object is gathering kettle metrics during execution
   */
  public boolean isGatheringMetrics();

  /**
   * Enable of disable kettle metrics gathering during execution
   *
   * @param gatheringMetrics
   *          set to true to enable metrics gathering during execution.
   */
  public void setGatheringMetrics( boolean gatheringMetrics );

  /**
   * This option will force the create of a separate logging channel even if the logging concerns identical objects with
   * identical names.
   *
   * @param forcingSeparateLogging
   *          Set to true to force separate logging
   */
  public void setForcingSeparateLogging( boolean forcingSeparateLogging );

  /**
   * @return True if the logging is forcibly separated out from even identical objects.
   */
  public boolean isForcingSeparateLogging();
}
