package ai.verta.modeldb.cron_jobs;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.rpc.Code;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimerTask;
import javax.persistence.OptimisticLockException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class CleanUpEntitiesCron extends TimerTask {
  private static final Logger LOGGER = LogManager.getLogger(CleanUpEntitiesCron.class);
  private final RoleService roleService;
  private final Integer recordUpdateLimit;

  public CleanUpEntitiesCron(RoleService roleService, Integer recordUpdateLimit) {
    this.roleService = roleService;
    this.recordUpdateLimit = recordUpdateLimit;
  }

  /** The action to be performed by this timer task. */
  @Override
  public void run() {
    LOGGER.info("CleanUpEntitiesCron wakeup");

    ModelDBUtils.registeredBackgroundUtilsCount();
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      // Clean up projects
      cleanProjects(session);

      // Clean up repositories
      cleanRepositories(session);
    } catch (Exception ex) {
      if (ex instanceof StatusRuntimeException) {
        StatusRuntimeException exception = (StatusRuntimeException) ex;
        if (exception.getStatus().getCode().value() == Code.PERMISSION_DENIED_VALUE) {
          LOGGER.warn("CleanUpEntitiesCron Exception: {}", ex.getMessage());
        } else {
          LOGGER.warn("CleanUpEntitiesCron Exception: ", ex);
        }
      } else {
        LOGGER.warn("CleanUpEntitiesCron Exception: ", ex);
      }
    } finally {
      ModelDBUtils.unregisteredBackgroundUtilsCount();
    }
    LOGGER.info("CleanUpEntitiesCron finish tasks and reschedule");
  }

  private void cleanProjects(Session session) {
    LOGGER.trace("Project cleaning");
    String alias = "pr";
    String deleteProjectsQueryString =
        new StringBuilder("FROM ")
            .append(ProjectEntity.class.getSimpleName())
            .append(" ")
            .append(alias)
            .append(" WHERE ")
            .append(alias)
            .append(".")
            .append(ModelDBConstants.CREATED)
            .append(" = :created ")
            .append(" AND ")
            .append(alias)
            .append(".")
            .append(ModelDBConstants.DATE_CREATED)
            .append(" < :created_date ")
            .toString();

    // Time less then a minute because possible to have create project request running when cron job
    // running
    long time = Calendar.getInstance().getTimeInMillis() - 300000; // 5 minute lesser time
    Query projectDeleteQuery = session.createQuery(deleteProjectsQueryString);
    projectDeleteQuery.setParameter("created", false);
    projectDeleteQuery.setParameter("created_date", time);
    projectDeleteQuery.setMaxResults(this.recordUpdateLimit);
    List<ProjectEntity> projectEntities = projectDeleteQuery.list();

    List<String> projectIds = new ArrayList<>();
    if (!projectEntities.isEmpty()) {
      for (ProjectEntity projectEntity : projectEntities) {
        projectIds.add(projectEntity.getId());
      }

      try {
        roleService.deleteEntityResources(
            projectIds, ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT);
        for (ProjectEntity projectEntity : projectEntities) {
          try {
            Transaction transaction = session.beginTransaction();
            session.delete(projectEntity);
            transaction.commit();
          } catch (OptimisticLockException ex) {
            LOGGER.info("CleanUpEntitiesCron : cleanProjects : Exception: {}", ex.getMessage());
          }
        }
      } catch (OptimisticLockException ex) {
        LOGGER.info("CleanUpEntitiesCron : cleanProjects : Exception: {}", ex.getMessage());
      } catch (Exception ex) {
        LOGGER.warn("CleanUpEntitiesCron : cleanProjects : Exception: ", ex);
      }
    }

    LOGGER.debug("Project cleaned successfully : Cleaned projects count {}", projectIds.size());
  }

  private void cleanRepositories(Session session) {
    LOGGER.trace("Repository cleaning");
    String alias = "r";
    String deleteRepositoriesQueryString =
        new StringBuilder("FROM ")
            .append(RepositoryEntity.class.getSimpleName())
            .append(" ")
            .append(alias)
            .append(" WHERE ")
            .append(alias)
            .append(".")
            .append(ModelDBConstants.CREATED)
            .append(" = :created ")
            .append(" AND ")
            .append(alias)
            .append(".")
            .append(ModelDBConstants.DATE_CREATED)
            .append(" < :created_date ")
            .toString();

    // Time less then a minute because possible to have create project request running when cron job
    // running
    long time = Calendar.getInstance().getTimeInMillis() - 300000; // 5 minute lesser time
    Query repositoryDeleteQuery = session.createQuery(deleteRepositoriesQueryString);
    repositoryDeleteQuery.setParameter("created", false);
    repositoryDeleteQuery.setParameter("created_date", time);
    repositoryDeleteQuery.setMaxResults(this.recordUpdateLimit);
    List<RepositoryEntity> repositoryEntities = repositoryDeleteQuery.list();

    List<String> repositoryIds = new ArrayList<>();
    if (!repositoryEntities.isEmpty()) {
      for (RepositoryEntity repositoryEntity : repositoryEntities) {
        repositoryIds.add(String.valueOf(repositoryEntity.getId()));
      }

      try {
        roleService.deleteEntityResources(
            repositoryIds, ModelDBResourceEnum.ModelDBServiceResourceTypes.REPOSITORY);
        for (RepositoryEntity repositoryEntity : repositoryEntities) {
          try {
            Transaction transaction = session.beginTransaction();
            session.delete(repositoryEntity);
            transaction.commit();
          } catch (OptimisticLockException ex) {
            LOGGER.info("CleanUpEntitiesCron : cleanRepositories : Exception: {}", ex.getMessage());
          }
        }
      } catch (OptimisticLockException ex) {
        LOGGER.info("CleanUpEntitiesCron : cleanRepositories : Exception: {}", ex.getMessage());
      } catch (Exception ex) {
        LOGGER.warn("CleanUpEntitiesCron : cleanRepositories : Exception: ", ex);
      }
    }

    LOGGER.debug(
        "Repository cleaned successfully : Cleaned repositories count {}", repositoryIds.size());
  }
}
