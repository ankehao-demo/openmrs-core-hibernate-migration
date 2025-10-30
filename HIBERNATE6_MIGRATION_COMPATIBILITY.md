# Hibernate 6 Migration - Dependency Compatibility Matrix

## Overview
This document provides the compatibility matrix for upgrading OpenMRS Core from Hibernate 5.6.15.Final to Hibernate 6.4.x and associated dependencies.

## Target Versions (Phase 2)

### Core Dependencies

| Component | Current Version | Target Version | Compatibility Notes |
|-----------|----------------|----------------|---------------------|
| **Hibernate ORM** | 5.6.15.Final | **6.4.10.Final** | Latest stable in 6.4 series (released Aug 2024) |
| **Spring Framework** | 5.3.30 | **6.2.12** | Latest stable, supports Hibernate 6.x as JPA provider |
| **Hibernate Search** | 6.2.4.Final | **7.0.1.Final** | Compatible with Hibernate ORM 6.4 |
| **Infinispan** | 13.0.22.Final | **14.0.35.Final** | Latest stable with Hibernate 6 support |
| **Apache Lucene** | 8.11.2 | **9.8.0** | Required by Hibernate Search 7.0.1 |
| **Hibernate Validator** | 9.0.1.Final | **9.0.1.Final** | Already compatible (no change needed) |

### Critical Artifact Changes

| Old Artifact | New Artifact | Notes |
|--------------|--------------|-------|
| `infinispan-hibernate-cache-v53` | `infinispan-hibernate-cache-v60` | Hibernate 6.0+ cache provider |

## Breaking Changes (To Be Addressed in Later Phases)

### 1. Jakarta Persistence Migration
- **Change**: `javax.persistence.*` → `jakarta.persistence.*`
- **Impact**: All JPA annotations and imports need updating
- **Affected Files**: All files using JPA annotations (estimated 100+ files)
- **Example**: `api/src/main/java/org/openmrs/api/db/hibernate/HibernateVisitDAO.java` lines 12-15

### 2. Hibernate Type System Changes
- **Change**: `StandardBasicTypes` class removed
- **Impact**: Custom type mappings need updating
- **Affected Files**: 
  - `api/src/main/java/org/openmrs/api/db/hibernate/HibernateContextDAO.java` (line 35)

### 3. Spring ORM Package Changes
- **Change**: `org.springframework.orm.hibernate5.*` → JPA-based setup
- **Impact**: Session factory configuration needs updating to use JPA approach
- **Affected Files**:
  - `api/src/main/java/org/openmrs/api/db/hibernate/HibernateSessionFactoryBean.java` (line 40)

### 4. Criteria API Changes
- **Change**: Deprecated Criteria API removed, use JPA Criteria API
- **Impact**: Query construction patterns need updating in DAO implementations
- **Affected Files**: All DAO implementations using Hibernate Criteria API

## Java Requirements
- **Minimum Java Version**: Java 11
- **Supported Versions**: Java 11, 17, 21
- **Current OpenMRS**: Compatible (using Java 11+)

## Jakarta EE Requirements
- **Hibernate 6.4 requires**: Jakarta EE 10
- **Jakarta Persistence**: 3.1

## Research Sources
- Hibernate 6.4 Release Page: https://hibernate.org/orm/releases/6.4/
- Hibernate 6.0 Migration Guide: https://docs.jboss.org/hibernate/orm/6.0/migration-guide/
- Hibernate 6.4 Migration Guide: https://docs.jboss.org/hibernate/orm/6.4/migration-guide/
- Hibernate Search 7.0 Release: https://hibernate.org/search/releases/7.0/
- Spring Framework 6.1+ Hibernate Support: https://docs.spring.io/spring-framework/reference/6.1/data-access/orm/hibernate.html
- Infinispan Cache v60 Artifact: https://central.sonatype.com/artifact/org.infinispan/infinispan-hibernate-cache-v60

## Phase 2 Implementation Checklist

### Root `pom.xml` Updates
- [x] Update `hibernateVersion` property from `5.6.15.Final` to `6.4.10.Final`
- [x] Update `springVersion` property from `5.3.30` to `6.2.12`
- [x] Update `hibernateSearchVersion` property from `6.2.4.Final` to `7.0.1.Final`
- [x] Update `infinispanVersion` property from `13.0.22.Final` to `14.0.35.Final`
- [x] Update `luceneVersion` property from `8.11.2` to `9.8.0`
- [x] Replace `infinispan-hibernate-cache-v53` artifact with `infinispan-hibernate-cache-v60`

### Verification
- [x] Run `mvn clean compile` to verify dependency resolution
- [x] Confirm Maven can download all new dependencies
- [x] Compilation errors in code are EXPECTED and acceptable at this milestone

**Milestone 2.1 Status: ✅ REACHED**
- Maven successfully resolved and downloaded all Hibernate 6.4.10.Final dependencies
- Compilation errors occurred as expected due to `javax.persistence` → `jakarta.persistence` migration
- Additional fix applied: Updated `lucene-analyzers-phonetic` → `lucene-analysis-phonetic` for Lucene 9.x compatibility

## Next Phases (Not in Current Scope)
- Phase 3: Fix compilation errors (javax.persistence → jakarta.persistence imports)
- Phase 4: Update Hibernate type system usage (StandardBasicTypes replacements)
- Phase 5: Update Spring ORM configuration (hibernate5 → JPA-based setup)
- Phase 6: Update Criteria API usage throughout DAOs
- Phase 7: Testing and validation
