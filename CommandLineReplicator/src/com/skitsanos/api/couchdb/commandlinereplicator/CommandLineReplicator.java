package com.skitsanos.api.couchdb.commandlinereplicator;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skitsanos.api.CouchDb;
import com.skitsanos.api.ServerResponse;
import com.skitsanos.api.exceptions.CouchDbException;
import com.skitsanos.api.exceptions.InvalidServerResponseException;
import javax.mail.*;
import java.io.*;
import java.util.*;

public class CommandLineReplicator {
    // Pop3ToCouchDbImport.exe <pop3server> <pop3port> <useSsl> <username>
    // <password> <couchDBServer> <couchDBport> <couchDBdatabase>");

    private String pop3Server;
    private int pop3Port;
    private boolean useSsl;
    private String userName;
    private String password;
    private String couchDbServer;
    private int couchDbPort;
    private String couchDbDatabase;
    private Session session;
    private Store store;
    private String couchDbUserName;
    private String couchDbPassword;  
    
    private CouchDb couchDb;

    public CommandLineReplicator(String couchDbServer, int couchDbPort, String couchDbUserName, String couchDbPassword) {
        super();        
        this.couchDbServer = couchDbServer;
        this.couchDbPort = couchDbPort;        
        this.couchDbUserName = couchDbUserName;
        this.couchDbPassword = couchDbPassword;
        couchDb = new CouchDb(couchDbServer, couchDbPort, couchDbUserName, couchDbPassword);
    }    
    
    public void replicate(String source, String target, Boolean createIfNotExists) {
    	Map<String, Object> replicate = new HashMap<String, Object>();
    	
    	replicate.put("source", source);
    	replicate.put("target", target);
    	replicate.put("create_target", createIfNotExists);    	
    	try {
			couchDb.replicate(replicate);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public List<String> getDatabases() {
    	ServerResponse response = couchDb.getDatabases();    	
    	String dbs = response.data();
    	dbs = dbs.replaceAll("\"", "").replaceAll("\\[", "").replaceAll("\\]", "");    	
    	List<String> dbList = new ArrayList<String>(Arrays.asList(dbs.split(",")));
    	return dbList;
    }
    
    

    @SuppressWarnings("unchecked")
	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
        if (args.length < 3) {
            System.out.println("Missing command line arguments. java -jar commandlinereplicator.jar <replication mapping file> <couchDBServer> <couchDBport> <couchDBusername optional> <couchDBpassword optional>");
            return;
        }
        String cdbUser = new String();
        String cdbPassword = new String();
        if (args.length == 5) {
        	cdbUser = args[3];
        	cdbPassword = args[4];
        }
        File mappingFile = new File(args[0]);
        if (!mappingFile.exists()) {
        	System.out.println("Mapping file doesnt exist.");
        	return;
        }
        	
        ObjectMapper mapper = new ObjectMapper();
        
        
        Map<String, Object> replicationMap = mapper.readValue(new File(args[0]), new TypeReference<Map<String, Object>>() {
		});
        Boolean createIfNotExist = false;
        CommandLineReplicator replicator = new CommandLineReplicator(args[1], Integer.parseInt(args[2]), cdbUser, cdbPassword);
        if (replicationMap.containsKey("createIfNotExists"))
        	createIfNotExist = (Boolean) replicationMap.get("createIfNotExists");
        List<Map<String,Object>> mapping = null;
        if (replicationMap.containsKey("replicate")) {
        	mapping = (List<Map<String,Object>>)replicationMap.get("replicate");
        	for(Map<String, Object> map : mapping) {
        		String source = null, target = null;
        		if (map.containsKey("source"))
        			source = (String)map.get("source");
        		if (map.containsKey("target"))
        			target = (String)map.get("target");
        		target = target.toLowerCase();
        		if (source.equalsIgnoreCase("*") && !target.contains("localhost") && !target.contains("127.0.0.1")) {
        			List<String> localDbList = replicator.getDatabases();
        			for(String db: localDbList) {
        				replicator.replicate(db, target, createIfNotExist);
        			}
        		} else {
        			replicator.replicate(source, target, createIfNotExist);
        		}
        	}
        	
        }
        
        
    }

}
