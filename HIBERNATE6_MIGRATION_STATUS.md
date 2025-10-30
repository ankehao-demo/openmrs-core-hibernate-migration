# Hibernate 6 Migration Status

## Overview
This document tracks the progress of migrating openmrs-core from Hibernate 5.6.15.Final to Hibernate 6.4.10.Final.

## Completed Phases

### Phase 1: Dependency Analysis & Preparation ✅
- ✅ Researched Hibernate 6 migration guide and documented breaking changes
- ✅ Created compatibility matrix for all dependencies
- ✅ Created migration branch: `devin/1761808283-hibernate6-migration`

**Compatibility Matrix:**
| Component | Old Version | New Version | Status |
|-----------|------------|-------------|--------|
| Hibernate ORM | 5.6.15.Final | 6.4.10.Final | ✅ Updated |
| Spring Framework | 5.3.30 | 6.1.15 | ✅ Updated |
| Hibernate Search | 6.2.4.Final | 7.2.4.Final | ✅ Updated |
| Infinispan | 13.0.22.Final | 15.2.6.Final | ✅ Updated |
| Infinispan Hibernate Cache | v53 | v62 | ✅ Updated |
| Infinispan Spring | spring5-embedded | spring6-embedded | ✅ Updated |
| Jakarta Validation API | javax.validation 2.0.1.Final | jakarta.validation 3.0.2 | ✅ Updated |
| Hibernate Validator | 9.0.1.Final | 9.0.1.Final | ✅ Compatible (no change needed) |

### Phase 2: Core Dependency Upgrades ✅
Updated dependency versions in POM files:

**Root pom.xml (lines 1279-1285):**
- `hibernateVersion`: 5.6.15.Final → 6.4.10.Final
- `springVersion`: 5.3.30 → 6.1.15
- `hibernateSearchVersion`: 6.2.4.Final → 7.2.4.Final
- `infinispanVersion`: 13.0.22.Final → 15.2.6.Final

**Root pom.xml (lines 234-236):**
- Replaced `infinispan-hibernate-cache-v53` with `infinispan-hibernate-cache-v62`

**Root pom.xml (lines 214-216):**
- Replaced `infinispan-spring5-embedded` with `infinispan-spring6-embedded`

**Root pom.xml (lines 519-521):**
- Replaced `javax.validation:validation-api` with `jakarta.validation:jakarta.validation-api` (version 3.0.2)

**api/pom.xml (lines 137, 95):**
- Updated both Infinispan dependencies to v62 and spring6 versions

### Phase 3: Package Migration (javax → jakarta) ✅
- ✅ Replaced all `import javax.persistence` with `import jakarta.persistence` across 84 Java files
- ✅ Updated `hibernate.cfg.xml` properties:
  - `javax.persistence.validation.mode` → `jakarta.persistence.validation.mode`
  - `javax.persistence.sharedCache.mode` → `jakarta.persistence.sharedCache.mode`
- ✅ Verified no `javax.persistence` references remain in code (only 2 in Javadoc comments, which is acceptable)

## Current Status: ⚠️ Compilation Errors

Running `mvn clean compile` produces errors due to Hibernate 6 API breaking changes that go beyond the package migration. These need to be addressed in a Phase 4.

## Outstanding Breaking Changes (Phase 4)

### 1. Session.createSQLQuery() → Session.createNativeQuery()
**Hibernate 6 Change:** The `createSQLQuery()` method has been removed from the `Session` interface.

**Solution:** Replace all occurrences of `session.createSQLQuery(sql)` with `session.createNativeQuery(sql)`.

**Affected Files:**
- `HibernateConceptDAO.java` (6 occurrences)
- `HibernateEncounterDAO.java` (2 occurrences)
- `HibernateObsDAO.java` (1 occurrence)
- `HibernateOrderDAO.java` (1 occurrence)
- `HibernatePatientDAO.java` (2 occurrences)

**Example:**
```java
// Old (Hibernate 5)
Query query = session.createSQLQuery("SELECT * FROM table");

// New (Hibernate 6)
Query query = session.createNativeQuery("SELECT * FROM table");
```

### 2. SQLQuery Type → NativeQuery Type
**Hibernate 6 Change:** The `SQLQuery` interface has been removed.

**Solution:** Replace `SQLQuery` type declarations with `NativeQuery`.

**Affected Files:**
- `HibernateEncounterDAO.java` (2 occurrences)
- `HibernateObsDAO.java` (1 occurrence)

**Example:**
```java
// Old (Hibernate 5)
SQLQuery query = session.createSQLQuery(sql);

// New (Hibernate 6)
NativeQuery query = session.createNativeQuery(sql);
```

### 3. Legacy Criteria API Removed
**Hibernate 6 Change:** The legacy Hibernate Criteria API (`session.createCriteria()`, `Restrictions`, `Order`) has been completely removed.

**Solution:** Migrate to JPA Criteria API. This is the most complex change and requires rewriting queries.

**Affected Files:**
- `HibernatePatientDAO.java` (multiple methods using Criteria API)

**Example Migration:**
```java
// Old (Hibernate 5 Legacy Criteria API)
Criteria criteria = session.createCriteria(Patient.class);
criteria.add(Restrictions.eq("voided", false));
criteria.addOrder(Order.asc("patientId"));
List<Patient> results = criteria.list();

// New (Hibernate 6 JPA Criteria API)
CriteriaBuilder cb = session.getCriteriaBuilder();
CriteriaQuery<Patient> cq = cb.createQuery(Patient.class);
Root<Patient> root = cq.from(Patient.class);
cq.select(root)
  .where(cb.equal(root.get("voided"), false))
  .orderBy(cb.asc(root.get("patientId")));
List<Patient> results = session.createQuery(cq).getResultList();
```

**Required Imports:**
```java
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Predicate;
```

### 4. Cache API Method Signature Changes
**Hibernate 6 Change:** Several cache eviction methods have changed signatures or been renamed.

**Affected Methods in `HibernateContextDAO.java`:**
- `cache.evictEntity(Class, Serializable)` - Method signature changed
- `cache.evictEntityRegion(Class)` - Method removed or renamed
- `cache.evictCollectionRegions()` - Method removed or renamed

**Solution:** Review the Hibernate 6 Cache API documentation and update method calls accordingly:
```java
// Hibernate 6 Cache API
cache.evict(entityClass, id);  // New method signature
cache.evictEntityData(entityClass);  // Renamed method
cache.evictAllRegions();  // Renamed method
```

### 5. ScrollableResults API Changes
**Hibernate 6 Change:** The `ScrollableResults.get(int)` method has been changed to `ScrollableResults.get()` (no index parameter).

**Affected Files:**
- `HibernateContextDAO.java` (line 542)

**Solution:** 
```java
// Old (Hibernate 5)
Object value = scrollableResults.get(0);

// New (Hibernate 6)
Object[] row = scrollableResults.get();
Object value = row[0];
```

### 6. StandardBasicTypes Usage
**Status:** Likely Compatible ✅

According to Hibernate 6 migration guide, `StandardBasicTypes` usage is "mostly source compatible" with the new `BasicTypeReference` API.

**Affected Files:**
- `HibernateHL7DAO.java` (5 usages)
- `HibernateProgramWorkflowDAO.java` (2 usages)
- `HibernateContextDAO.java` (2 usages)

**Action:** Monitor during compilation. If issues arise, replace with `BasicTypeReference`.

## Migration Steps for Phase 4

### Step 1: Replace createSQLQuery() calls
```bash
# Use find_and_edit to replace all occurrences
find . -name "*.java" -type f -exec sed -i 's/\.createSQLQuery(/.createNativeQuery(/g' {} \;
```

### Step 2: Update SQLQuery type declarations
Replace all `SQLQuery` imports and type declarations with `NativeQuery`:
- Import: `org.hibernate.query.NativeQuery`
- Type: `NativeQuery<T>` or `NativeQuery`

### Step 3: Migrate Legacy Criteria API
For each file using Criteria API:
1. Add JPA Criteria API imports
2. Rewrite query using `CriteriaBuilder` and `CriteriaQuery`
3. Test the migrated query

### Step 4: Update Cache API calls
Review and update cache eviction method calls in `HibernateContextDAO.java`:
- Replace `evictEntity()` with `evict()`
- Replace `evictEntityRegion()` with `evictEntityData()`
- Replace `evictCollectionRegions()` with `evictAllRegions()`

### Step 5: Fix ScrollableResults usage
Update `HibernateContextDAO.java` line 542 to use the new API without index parameter.

### Step 6: Compile and test
```bash
mvn clean compile
mvn test
```

## Verification Steps

After completing Phase 4:

1. **Compile:** `mvn clean compile` - Should succeed with no errors
2. **Test:** `mvn test` - Should pass or have minimal failures
3. **Manual Testing:** 
   - Start the application: `cd webapp && mvn jetty:run`
   - Verify core functionality works
   - Test database operations
   - Check caching behavior

## Known Issues and Risks

### High Priority
- **Legacy Criteria API migration** is the most complex change and may introduce bugs if not carefully tested
- **Cache API changes** may affect performance or caching behavior
- **Query refactoring** could introduce subtle differences in query behavior

### Medium Priority
- **StandardBasicTypes** may need updates despite being "mostly compatible"
- **ScrollableResults** changes could affect large result set handling
- Some **test fixtures** may need updates to work with Hibernate 6

### Low Priority
- **Warning messages** during compilation (Hibernate artifact relocation warnings are informational only)
- **Deprecated APIs** may still work but should be addressed eventually

## References

- [Hibernate 6.0 Migration Guide](https://docs.hibernate.org/orm/6.0/migration-guide/migration-guide.html)
- [Hibernate 6.4 Migration Guide](https://docs.hibernate.org/orm/6.4/migration-guide/migration-guide.html)
- [Spring Framework 6.1 Documentation](https://docs.spring.io/spring-framework/docs/6.1.x/reference/html/)
- [Hibernate Search 7.2 Migration Guide](https://docs.jboss.org/hibernate/search/7.2/migration/html_single/)
- [Jakarta Persistence 3.1 Specification](https://jakarta.ee/specifications/persistence/3.1/)

## Session Information

- **Devin Session:** https://app.devin.ai/sessions/b54c23be18a442ab986579a64e521839
- **Requested by:** @ankehao-demo
- **Branch:** `devin/1761808283-hibernate6-migration`
- **Base Branch:** `master`
