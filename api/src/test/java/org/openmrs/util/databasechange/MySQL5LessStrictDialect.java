/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.util.databasechange;

import org.hibernate.dialect.MySQLDialect;

/**
 * Note: In Hibernate 6, the column type registration API changed significantly.
 * This dialect now simply extends MySQLDialect without custom type mappings.
 * The validation logic may need to be updated to be more lenient with type differences.
 */
public class MySQL5LessStrictDialect extends MySQLDialect {
	
	public MySQL5LessStrictDialect() {
		super();
	}
}
