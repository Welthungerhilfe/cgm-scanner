package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.content.Context;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.models.Workflow;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;

public class WorkflowRepository {

    private static WorkflowRepository instance;
    private CgmDatabase database;
    private SessionManager session;
    private boolean updated;

    private WorkflowRepository(Context context) {
        database = CgmDatabase.getInstance(context);
        session = new SessionManager(context);
    }

    public static WorkflowRepository getInstance(Context context) {
        if (instance == null) {
            instance = new WorkflowRepository(context);
        }
        return instance;
    }
    public void insertWorkflow(Workflow workflow) {
        database.workfolwDao().insertPerson(workflow);
    }

    public String getWorkFlowId(String name, String version, int environment){
        return database.workfolwDao().getWorkFlowId(name,version,environment);
    }

}
