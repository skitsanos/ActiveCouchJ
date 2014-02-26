package com.skitsanos.api.couchdb.pop3tocouchdbimport;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skitsanos.api.CouchDb;
import com.skitsanos.api.DocumentInfo;
import com.sun.mail.pop3.POP3Store;

import javax.activation.MimetypesFileTypeMap;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class Pop3ToCouchDbImport {
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

    public Pop3ToCouchDbImport(String pop3Server, int pop3Port, boolean useSsl, String userName, String password, String couchDbServer, int couchDbPort, String couchDbDatabase) {
        super();
        this.pop3Server = pop3Server;
        this.pop3Port = pop3Port;
        this.useSsl = useSsl;
        this.userName = userName;
        this.password = password;
        this.couchDbServer = couchDbServer;
        this.couchDbPort = couchDbPort;
        this.couchDbDatabase = couchDbDatabase;
    }

    public Pop3ToCouchDbImport(String pop3Server, int pop3Port, boolean useSsl, String userName, String password, String couchDbServer, int couchDbPort, String couchDbDatabase, String couchDbUserName, String couchDbPassword) {
        super();
        this.pop3Server = pop3Server;
        this.pop3Port = pop3Port;
        this.useSsl = useSsl;
        this.userName = userName;
        this.password = password;
        this.couchDbServer = couchDbServer;
        this.couchDbPort = couchDbPort;
        this.couchDbDatabase = couchDbDatabase;
        this.couchDbUserName = couchDbUserName;
        this.couchDbPassword = couchDbPassword;
    }

    public void getEmails() {
        try {
            System.out.println("Connecting to " + pop3Server + "...");
            if (useSsl) {

                String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

                Properties pop3Props = new Properties();

                pop3Props.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);
                pop3Props.setProperty("mail.pop3.socketFactory.fallback", "false");
                pop3Props.setProperty("mail.pop3.port", Integer.valueOf(pop3Port).toString());
                pop3Props.setProperty("mail.pop3.socketFactory.port", Integer.valueOf(pop3Port).toString());

                URLName url = new URLName("pop3", pop3Server, pop3Port, "", userName, password);

                session = Session.getInstance(pop3Props, null);
                store = new POP3Store(session, url);
                store.connect();
            } else {
                session = Session.getInstance(new Properties());
                store = session.getStore("pop3");
                store.connect(pop3Server, pop3Port, userName, password);
            }
            if (store.isConnected())
                System.out.println(" -- Connected to " + pop3Server);
            else
                System.out.println("Can't connect to " + pop3Server);

            System.out.println(" -- Checking messages...");
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            int count = folder.getMessageCount();
            System.out.println(" -- " + count + " messages found. Retrieving...");
            Message[] messages = folder.getMessages();
            FetchProfile fetchProfile = new FetchProfile();
            fetchProfile.add(FetchProfile.Item.ENVELOPE);
            fetchProfile.add(UIDFolder.FetchProfileItem.UID);
            fetchProfile.add(FetchProfile.Item.CONTENT_INFO);
            folder.fetch(messages, fetchProfile);
            CouchDb couchDb = new CouchDb(couchDbServer, couchDbPort, couchDbUserName, couchDbPassword);
            ObjectMapper mapper = new ObjectMapper();
            for (int i = 0; i < messages.length; i++) {
                Message message = messages[i];
                String uid = new String();
                if (folder instanceof com.sun.mail.pop3.POP3Folder) {
                    com.sun.mail.pop3.POP3Folder pf = (com.sun.mail.pop3.POP3Folder) folder;
                    uid = pf.getUID(messages[i]);
                }
                System.out.print("Parsing message " + String.format("%s", uid) + " ");
                Address[] from = message.getFrom();
                if (from.length > 0) {
                    InternetAddress iAddress = (InternetAddress) from[0];
                    System.out.print(iAddress.getAddress());
                }
                System.out.println(" " + message.getSentDate());
                System.out.println(message.getSubject());
                Address[] to = message.getRecipients(Message.RecipientType.TO);
                Address[] cc = message.getRecipients(Message.RecipientType.CC);
                // Address[] bcc =
                // message.getRecipients(Message.RecipientType.BCC);
                String subject = message.getSubject();
                Object content = message.getContent();                
                StringBuilder bodyContent = new StringBuilder();
                List<File> fileList = new ArrayList<File>();
                if (content instanceof String) {
                    bodyContent.append((String) content);
                } else if (content instanceof Multipart) {
                    handleMultipart((Multipart) content, bodyContent, fileList);
                } else {
                    handlePart(message, bodyContent, fileList);
                }
                Map<String, Object> messageMap = new HashMap<String, Object>();
                List<Map<String, String>> fromList = new ArrayList<Map<String, String>>();
                List<Map<String, String>> toList = new ArrayList<Map<String, String>>();
                List<Map<String, String>> ccList = new ArrayList<Map<String, String>>();
                if (null != from)
                    for (Address frm : from) {
                        Map<String, String> address = new HashMap<String, String>();
                        InternetAddress iAddress = (InternetAddress) frm;
                        address.put("address", iAddress.getAddress());
                        address.put("displayName", iAddress.getPersonal());
                        fromList.add(address);
                    }
                if (null != to)
                    for (Address t : to) {
                        Map<String, String> address = new HashMap<String, String>();
                        InternetAddress iAddress = (InternetAddress) t;
                        address.put("address", iAddress.getAddress());
                        address.put("displayName", iAddress.getPersonal());
                        toList.add(address);
                    }
                if (null != cc)
                    for (Address c : cc) {
                        Map<String, String> address = new HashMap<String, String>();
                        InternetAddress iAddress = (InternetAddress) c;
                        address.put("address", iAddress.getAddress());
                        address.put("displayName", iAddress.getPersonal());
                        ccList.add(address);
                    }

                messageMap.put("uid", uid);
                messageMap.put("size", message.getSize());
                messageMap.put("date", message.getSentDate());
                messageMap.put("importance", message.getHeader("X-Priority"));
                messageMap.put("from", fromList);
                messageMap.put("to", toList);
                messageMap.put("cc", ccList);                
                messageMap.put("body", mapper.writeValueAsString(bodyContent.toString()));

                try {
                    System.out.println(" -- Writing to CouchDb...");
                    DocumentInfo docInfo = couchDb.createDocument(couchDbDatabase, messageMap);

                    if (fileList.size() > 0) {
                        for (File file : fileList) {
                            System.out.println(" -- Processing attachment " + new MimetypesFileTypeMap().getContentType(file));
                            String fileName = file.getName();
                            if (fileName.startsWith("_"))
                            	fileName = fileName.substring(1);
                            couchDb.setInlineAttachment(couchDbDatabase, docInfo.id, fileName, new MimetypesFileTypeMap().getContentType(file), Files.readAllBytes(file.toPath()));
                            file.delete();
                        }
                    }
                    System.out.println(" -- saved");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            folder.close(false);
            store.close();
            System.out.println("Mailbox import completed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleMultipart(Multipart multipart, StringBuilder bodyContent, List<File> fileList) throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            handlePart(multipart.getBodyPart(i), bodyContent, fileList);
        }
    }

    public void handlePart(Part part, StringBuilder bodyContent, List<File> fileList) throws MessagingException, IOException {
        String dposition = part.getDisposition();
        String cType = part.getContentType();

        if ((dposition == null) && (part.isMimeType("text/plain") || part.isMimeType("text/html"))) {
            System.out.println("Null: " + cType);
            bodyContent.append((String) part.getContent());
            bodyContent.append(System.lineSeparator());
        } else if (part.isMimeType("multipart/*")) {
            handleMultipart((Multipart) part.getContent(), bodyContent, fileList);
        } else if (dposition.equalsIgnoreCase(Part.ATTACHMENT)) {
//			System.out.println("Attachment: " + part.getFileName() + " : " + cType);
            fileList.add(saveFile(part.getFileName(), part.getInputStream()));
        } else if (dposition.equalsIgnoreCase(Part.INLINE)) {
            //		System.out.println("Inline: " + part.getFileName() + " : " + cType);
            fileList.add(saveFile(part.getFileName(), part.getInputStream()));
        } else {
            //	System.out.println("Other: " + dposition);
        }
    }

    public File saveFile(String filename, InputStream input) throws IOException {
        if (filename == null) {
            filename = File.createTempFile("Inline-", ".out").getName();
        }
        //System.out.println("downloading attachment...");
        File file = new File(filename);
        for (int i = 0; file.exists(); i++) {
            file = new File(filename + i);
        }
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        BufferedInputStream bis = new BufferedInputStream(input);
        int fByte;
        while ((fByte = bis.read()) != -1) {
            bos.write(fByte);
        }
        bos.flush();
        bos.close();
        bis.close();
        //System.out.println("done attachment...");
        return file;
    }

    public static void main(String[] args) {
        if (args.length < 8) {
            System.out.println("Missing command line arguments. Pop3ToCouchDbImport.exe <pop3server> <pop3port> <useSsl> <username> <password> " +
                    "<couchDBServer> <couchDBport> <couchDBdatabase> <couchDBusername optional> <couchDBpassword optional>");
            return;
        }
        String cdbUser = new String();
        String cdbPassword = new String();
        if (args.length == 10) {
        	cdbUser = args[8];
        	cdbPassword = args[9];
        }
        	
        Pop3ToCouchDbImport pop3 = new Pop3ToCouchDbImport(args[0], Integer.parseInt(args[1]), Boolean.parseBoolean(args[2]), args[3], args[4], args[5], Integer.parseInt(args[6]), args[7], cdbUser, cdbPassword);
        pop3.getEmails();
    }

}
