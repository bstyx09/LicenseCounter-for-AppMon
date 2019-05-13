package licensecounter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Benjamin Styx benjamin.styx@dynatrace.com
 *
 */
public class LicenseCounter {

    static String textOutput;
    static Integer[][] finalSPLicenseCount;
    static Integer[][] finalAGLicenseCount;
    static List<String> systemProfileList = new ArrayList<>();
    static List<String> dotNetHostList = new ArrayList<>();
    static List<String> webServerHostList = new ArrayList<>();
    static List<String> agentGroupList = new ArrayList<>();
    static List<String> agentTechs = new ArrayList<>();
    static List<Integer> javaAgentCount = new ArrayList<>();
    static List<Integer> javaMicroserviceCount = new ArrayList<>();
    static String[][] agentProperty;
    static String[] agentInformation;
    static StringBuilder sb = new StringBuilder();
    static String[][] association;

    //main method
    public static void main(String[] args) {

        //args = new String[1];
        //args[0] = "c:/new folder/agents prod.xml";
        //pull in data from file
        readFromFile(args[0]);
        Collections.sort(systemProfileList);
        sortAgents();

        for (int i = 0; i < agentInformation.length; i++) {
            agentProperty[i] = agentInformation[i].split("><");
        }

        //also add a column for MicroService licenses
        agentTechs.add(("MicroService"));

        //initialize arrays based on number of system profiles and number of different types of technologies
        finalSPLicenseCount = new Integer[systemProfileList.size()][agentTechs.size()];
        finalAGLicenseCount = new Integer[agentGroupList.size()][agentTechs.size()];

        for (int i = 0; i < systemProfileList.size(); i++) {
            javaMicroserviceCount.add(0);
            javaAgentCount.add(0);
            for (int j = 0; j < agentTechs.size(); j++) {
                finalSPLicenseCount[i][j] = 0;
            }
        }
        for (int i = 0; i < agentGroupList.size(); i++) {
            for (int j = 0; j < agentTechs.size(); j++) {
                finalAGLicenseCount[i][j] = 0;
            }
        }

        parseData();

        sb.append(System.lineSeparator());
        sb.append("--------------------------------------------------------------------------");
        sb.append(System.lineSeparator());
        sb.append("Java agent usage by system profile: ");
        sb.append(System.lineSeparator());
        sb.append("--------------------------------------------------------------------------");

        int totalJavaCount = 0;
        int totalMicroCount = 0;

        for (int i = 0; i < systemProfileList.size(); i++) {
            sb.append(System.lineSeparator());
            sb.append("System profile: " + systemProfileList.get(i));
            sb.append(System.lineSeparator());
            sb.append("Total Java agents: " + javaAgentCount.get(i));
            sb.append(System.lineSeparator());
            sb.append("Java agents under 2GB heap: " + javaMicroserviceCount.get(i));
            sb.append(System.lineSeparator());
            totalJavaCount = totalJavaCount + javaAgentCount.get(i);
            totalMicroCount = totalMicroCount + javaMicroserviceCount.get(i);
        }

        sb.append(System.lineSeparator());
        sb.append(System.lineSeparator());

        //get number of java agents
        sb.append("Number of Java agents = " + totalJavaCount);
        sb.append(System.lineSeparator());
        sb.append("Number of Java agents capable of using microservice licenses = " + totalMicroCount);
        sb.append(System.lineSeparator());
        textOutput = sb.toString();
        System.out.print(textOutput);
        try {
            writeToTextFile(args[0]);
            writeToCSVFile(args[0]);
        } catch (Exception e) {

        }

    }

    public static void parseData() {
        //int countOfJavaAgents = 0;
        //int countOfMicroserviceAgents = 0;

        for (int i = 0; i < agentInformation.length; i++) {
            /*
             *
             *
             * look for java agents
             *
             *
             */
            if (agentInformation[i].contains("technologyType>Java</technologyType") && agentInformation[i].contains("Instrumentation enabled")) {
                //new line for each property of each agent
                //countOfJavaAgents++;
                boolean underMemThreshold = false;
                String agentGroupName = "";
                //System.out.print(splitLineAgain[i][0]);
                for (int j = 0; j < agentProperty[i].length; j++) {

                    if (agentProperty[i][j].contains("agentGroup>")) {
                        String currentProperty = agentProperty[i][j];
                        agentGroupName = currentProperty.substring(11, currentProperty.indexOf("<"));
                    }
                    if (agentProperty[i][j].contains("maximumMemory>")) {
                        String myString = agentProperty[i][j];
                        String memorySize = myString.substring(14, (myString.indexOf("<")));
                        double heapSize = Double.parseDouble(memorySize);
                        heapSize = (heapSize / 1073741824);
                        //System.out.println(heapSize);
                        if (heapSize <= 2) {
                            underMemThreshold = true;
                            //countOfMicroserviceAgents++;
                            sb.append("Heap size = " + heapSize + "GB");
                            sb.append(System.lineSeparator());
                            int microServiceIndex = agentTechs.indexOf("MicroService");
                            int javaIndex = agentTechs.indexOf("Java");
                            int groupIndex = agentGroupList.indexOf(agentGroupName);
                            finalAGLicenseCount[groupIndex][microServiceIndex]++;
                            finalAGLicenseCount[groupIndex][javaIndex]++;

                        } else {
                            sb.append("Heap size = " + heapSize + "GB");
                            sb.append(System.lineSeparator());
                            int javaIndex = agentTechs.indexOf("Java");
                            int groupIndex = agentGroupList.indexOf(agentGroupName);
                            finalAGLicenseCount[groupIndex][javaIndex] = finalAGLicenseCount[groupIndex][javaIndex]++;

                        }
                    } else if (agentProperty[i][j].contains("agentInstanceName>")) {
                        String myString = agentProperty[i][j];
                        sb.append("Agent Name = " + myString.substring(18, myString.indexOf("<")));
                        sb.append(System.lineSeparator());

                    } else if (agentProperty[i][j].contains("instrumentationState>")) {
                        String myString = agentProperty[i][j];
                        //sb.append("Instrumentation State =  " + myString.substring(21, myString.indexOf("<")));
                        //sb.append(System.lineSeparator());

                    } else if (agentProperty[i][j].contains("systemProfile>")) {
                        String myString = agentProperty[i][j];
                        String systemProfileName = myString.substring(14, myString.indexOf("<"));

                        sb.append("System Profile =  " + systemProfileName);
                        sb.append(System.lineSeparator());
                        sb.append(System.lineSeparator());

                        //see if this is already in the list
                        if (systemProfileList.contains(systemProfileName)) {
                            int k = systemProfileList.indexOf(systemProfileName);
                            //System Profile exists, increment it's number of agents

                            if (underMemThreshold) {
                                int previousValue = javaMicroserviceCount.get(k);
                                javaMicroserviceCount.set(k, previousValue + 1);
                                int previousValue2 = javaAgentCount.get(k);
                                javaAgentCount.set(k, previousValue2 + 1);

                                //add to real counter
                                int microServiceIndex = agentTechs.indexOf("MicroService");
                                int profileIndex = systemProfileList.indexOf(systemProfileName);
                                int javaIndex = agentTechs.indexOf("Java");
                                finalSPLicenseCount[profileIndex][javaIndex]++;
                                finalSPLicenseCount[profileIndex][microServiceIndex]++;
                            } else {
                                int previousValue = javaAgentCount.get(k);
                                javaAgentCount.set(k, previousValue + 1);
                                int profileIndex = systemProfileList.indexOf(systemProfileName);
                                int javaIndex = agentTechs.indexOf("Java");
                                finalSPLicenseCount[profileIndex][javaIndex]++;
                            }
                        }

                    } else if (agentProperty[i][j].contains("agentGroup>")) {
                        String myString = agentProperty[i][j];
                        sb.append("Agent Group =  " + myString.substring(11, myString.indexOf("<")));
                        sb.append(System.lineSeparator());
                    }
                    //}
                }
            } else if (agentInformation[i].contains("Instrumentation enabled")) {
                /*
                
                 look for other types of agents
                
                 */

                String systemProfileName = null;
                String agentGroupName = null;
                String hostName = null;
                //look at each agent field
                for (int m = 0; m < agentProperty[i].length; m++) {
                    String thisString = agentProperty[i][m];

                    if (thisString.contains("agentGroup>")) {
                        agentGroupName = thisString.substring(11, thisString.indexOf("<"));
                    }

                    //look for system profile name
                    if (thisString.contains("systemProfile>")) {
                        systemProfileName = thisString.substring(14, thisString.indexOf("<"));
                    }

                    //get the host name
                    if (thisString.contains("agentHost>")) {
                        hostName = thisString.substring(10, thisString.indexOf("<"));
                    }

                    //look for the technology type
                    if (thisString.contains("technologyType>")) {
                        String currentTech = thisString.substring(15, thisString.indexOf("<"));

                        int profileIndex = systemProfileList.indexOf(systemProfileName);
                        int techIndex = agentTechs.indexOf(currentTech);
                        int groupIndex = agentGroupList.indexOf(agentGroupName);

                        //make sure there are no duplicate .NET hosts
                        if (currentTech.equalsIgnoreCase(".NET")) {
                            if (dotNetHostList.contains(hostName)) {

                            } else {
                                dotNetHostList.add(hostName);
                                finalSPLicenseCount[profileIndex][techIndex]++;
                                finalAGLicenseCount[groupIndex][techIndex]++;
                            }
                            //make sure there are no duplicate web server hosts
                        } else if (currentTech.equalsIgnoreCase("Web Server")) {
                            if (webServerHostList.contains(hostName)) {

                            } else {
                                webServerHostList.add(hostName);
                               finalSPLicenseCount[profileIndex][techIndex]++;
                                finalAGLicenseCount[groupIndex][techIndex]++;
                            }
                        } else {
                            finalSPLicenseCount[profileIndex][techIndex]++;

                            finalAGLicenseCount[groupIndex][techIndex]++;
                            //
                            //if this isn't in the list already
                        }
                    }
                }

            }

        }
    }

    public static void readFromFile(String fileName) {

        //open the XML file
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {

            String line = br.readLine();
            String entireFile = null;
            //pull everything from the file
            while (line != null) {
                entireFile = entireFile + line;
                line = br.readLine();
            }

            //new line for each different agent
            agentInformation = entireFile.split("<agentinformation>");

            agentProperty = new String[agentInformation.length][];
            for (int i = 0; i < agentInformation.length; i++) {

                agentProperty[i] = agentInformation[i].split("><");

                //count the system profiles and agent technology types
                for (int j = 0; j < agentProperty[i].length; j++) {
                    if (agentInformation[i].contains("Instrumentation enabled")) {
                        //look for tech types and system profiles
                        String currentString = agentProperty[i][j];

                        if (currentString.contains("technologyType>")) {
                            String techType = currentString.substring(15, currentString.indexOf("<"));
                            if (agentTechs.isEmpty()) {
                                agentTechs.add(techType);
                            } else if (agentTechs.contains(techType)) {
                                //do nothing, this already exists!
                            } else {
                                agentTechs.add(techType);
                            }

                        } else if (currentString.contains("systemProfile>")) {

                            String systemProfileName = currentString.substring(14, currentString.indexOf("<"));
                            if (systemProfileList.isEmpty()) {
                                //
                                systemProfileList.add(systemProfileName);
                            } else if (systemProfileList.contains(systemProfileName)) {

                            } else {
                                systemProfileList.add(systemProfileName);
                            }
                        } else if (currentString.contains("agentGroup>")) {

                            String agentGroupName = currentString.substring(11, currentString.indexOf("<"));
                            if (agentGroupList.isEmpty()) {
                                //
                                agentGroupList.add(agentGroupName);
                            } else if (agentGroupList.contains(agentGroupName)) {

                            } else {
                                agentGroupList.add(agentGroupName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            //do nothing
        }
    }

    public static void sortAgents() {
        List<String> sortedAgents = new ArrayList<>();
        for (int i = 0; i < systemProfileList.size(); i++) {
            for (int j = 0; j < agentInformation.length; j++) {
                if (agentInformation[j].contains("<systemProfile>")) {
                    int beginIndex = agentInformation[j].indexOf("<systemProfile>");
                    beginIndex = beginIndex + 15;
                    int endIndex = agentInformation[j].indexOf("</systemProfile>", beginIndex);
                    String curSysProf = agentInformation[j].substring(beginIndex, endIndex);
                    if (curSysProf.equals(systemProfileList.get(i))) {
                        sortedAgents.add(agentInformation[j]);
                    }
                }
            }
        }
        for (int i = 0; i < sortedAgents.size(); i++) {
            agentInformation[i] = sortedAgents.get(i);
        }
        System.out.println();
    }

    public static void writeToTextFile(String args) throws Exception {
        try {
            PrintWriter writer = new PrintWriter(args + "-output.txt", "UTF-8");
            writer.println(textOutput);
            writer.close();
        } catch (IOException e) {
            // do something
        }
    }

    public static void writeToCSVFile(String args) throws Exception {
        try {
            PrintWriter writerSP = new PrintWriter(args + "-output.csv", "UTF-8");
            writerSP.print(" ,");
            for (int i = 0; i < agentTechs.size(); i++) {
                writerSP.print(agentTechs.get(i) + ",");
            }
            writerSP.println();
            for (int i = 0; i < finalSPLicenseCount.length; i++) {
                writerSP.print(systemProfileList.get(i) + ",");
                for (int j = 0; j < finalSPLicenseCount[i].length; j++) {
                    writerSP.print(finalSPLicenseCount[i][j] + ",");
                }
                writerSP.println();
            }
            writerSP.close();

            PrintWriter writerAG = new PrintWriter(args + "-AgentGroupoutput.csv", "UTF-8");
            writerAG.print(" ,");
            for (int i = 0; i < agentTechs.size(); i++) {
                writerAG.print(agentTechs.get(i) + ",");
            }
            writerAG.println();
            for (int i = 0; i < finalAGLicenseCount.length; i++) {
                writerAG.print(agentGroupList.get(i) + ",");
                for (int j = 0; j < finalAGLicenseCount[i].length; j++) {
                    writerAG.print(finalAGLicenseCount[i][j] + ",");
                }
                writerAG.println();
            }
            writerAG.close();
        } catch (Exception e) {
            // do something
        }
    }

}
