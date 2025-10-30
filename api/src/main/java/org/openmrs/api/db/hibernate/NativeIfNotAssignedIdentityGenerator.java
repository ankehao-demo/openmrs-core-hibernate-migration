/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.api.db.hibernate;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * <b>native-if-not-assigned</b><br>
 * <br>
 * By setting the Hibernate configuration's primary key column to use a "native" implementation,
 * Hibernate ALWAYS generates the entity's id when it is being saved. There is no way to "override"
 * the generated id. <br>
 * <br>
 * This IdentityGenerator allows a programmer to override the "generated" id, with an "assigned" id
 * at runtime by simply setting the primary key property.
 * 
 * @author paul.shemansky@gmail.com
 */
public class NativeIfNotAssignedIdentityGenerator implements IdentifierGenerator, Configurable {
	
	private String entityName;
	
	private String sequenceName;
	
	@Override
	public Object generate(SharedSessionContractImplementor session, Object entity) throws HibernateException {
		EntityPersister persister = session.getEntityPersister(entityName, entity);
		Object id = persister.getIdentifier(entity, session);
		if (id == null && sequenceName != null) {
			try {
				id = session.doReturningWork(connection -> {
					String dialectName = session.getFactory().getJdbcServices().getDialect().getClass().getSimpleName();
					
					try (Statement stmt = connection.createStatement()) {
						String checkSeq = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_NAME = '" + sequenceName.toUpperCase() + "'";
						ResultSet rs = stmt.executeQuery(checkSeq);
						boolean exists = false;
						if (rs.next()) {
							exists = rs.getInt(1) > 0;
						}
						rs.close();
						
						if (!exists) {
							stmt.execute("CREATE SEQUENCE " + sequenceName + " START WITH 1 INCREMENT BY 1");
							
							String tableName = entityName.substring(entityName.lastIndexOf('.') + 1).toLowerCase();
							String idColumn = tableName + "_id";
							String maxIdQuery = "SELECT COALESCE(MAX(" + idColumn + "), 0) FROM " + tableName;
							ResultSet maxRs = stmt.executeQuery(maxIdQuery);
							int maxId = 0;
							if (maxRs.next()) {
								maxId = maxRs.getInt(1);
							}
							maxRs.close();
							
							if (maxId > 0) {
								stmt.execute("ALTER SEQUENCE " + sequenceName + " RESTART WITH " + (maxId + 1));
							}
						}
					} catch (Exception e) {
					}
					
					String sql;
					if (dialectName.contains("PostgreSQL") || dialectName.contains("Postgres")) {
						sql = "SELECT nextval('" + sequenceName + "')";
					} else if (dialectName.contains("H2")) {
						sql = "SELECT NEXT VALUE FOR " + sequenceName;
					} else if (dialectName.contains("Oracle")) {
						sql = "SELECT " + sequenceName + ".nextval FROM dual";
					} else {
						sql = "SELECT NEXT VALUE FOR " + sequenceName;
					}
					
					try (Statement stmt = connection.createStatement();
					     ResultSet rs = stmt.executeQuery(sql)) {
						if (rs.next()) {
							return rs.getInt(1);
						}
					}
					return null;
				});
			} catch (Exception e) {
				return null;
			}
		}
		return id;
	}

	@Override
	public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
		this.entityName = params.getProperty(IdentifierGenerator.ENTITY_NAME);
		if (entityName == null) {
			throw new MappingException("no entity name");
		}
		
		this.sequenceName = params.getProperty("sequence");
	}
}
