/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package com.openshift.restclient.model.user;

import java.util.Set;

import com.openshift.restclient.model.IResource;

public interface IUser extends IResource {

    /**
     * The full name of this user
     * 
     */
    String getFullName();

    /**
     * Returns the user uid as specified in the metadata
     * 
     */
    String getUID();

    /**
     * Returns the name of the groups this user belongs to
     *
     */
    Set<String> getGroups();

    /**
     * Returns the identities that point to this user
     *
     */
    Set<String> getIdentities();
}
