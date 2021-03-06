/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package jo.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.ib.client.Types.Method;

import jo.controller.model.Alias;
import jo.controller.model.Group;
import jo.controller.model.Profile;
import jo.controller.model.Profile.Allocation;
import jo.controller.model.Profile.Type;

public class AdvisorUtil {
    static List<Group> getGroups(String xml) {
        try {
            return getGroups_(xml);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static List<Group> getGroups_(String xml) throws IOException {
        ArrayList<Group> list = new ArrayList<Group>();

        Group group = null;

        BufferedReader reader = new BufferedReader(new StringReader(xml));
        String line;
        int state = 0; // 0=none; 1=list of groups; 2=reading group 3=listOfAccts
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            switch (state) {
            // top of file
            case 0:
                if (line.equals("<ListOfGroups>")) {
                    state = 1;
                }
                break;

            // reading groups
            case 1:
                if (line.equals("<Group>")) {
                    group = new Group();
                    state = 2;
                } else if (line.equals("</ListOfGroups>")) {
                    state = 0;
                } else {
                    err(line);
                }
                break;

            // reading group
            case 2:
                if (line.startsWith("<name>")) {
                    group.setName(getVal(line));
                } else if (line.startsWith("<defaultMethod>")) {
                    group.setDefaultMethod(Method.valueOf(getVal(line)));
                } else if (line.startsWith("<ListOfAccts")) {
                    state = 3;
                } else if (line.equals("</Group>")) {
                    list.add(group);
                    state = 1;
                } else {
                    err(line);
                }
                break;

            // reading list of accts
            case 3:
                if (line.equals("</ListOfAccts>")) {
                    state = 2;
                } else {
                    group.addAccount(getVal(line));
                }
                break;
            }
        }

        return list;
    }

    static List<Profile> getProfiles(String xml) {
        try {
            return getProfiles_(xml);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static List<Profile> getProfiles_(String xml) throws IOException {
        ArrayList<Profile> list = new ArrayList<Profile>();

        Profile profile = null;
        Allocation alloc = null;

        BufferedReader reader = new BufferedReader(new StringReader(xml));
        String line;
        int state = 0; // 0=none; 1=list of groups; 2=reading group 3=listOfAllocations 4=allocation
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            switch (state) {
            // top of file
            case 0:
                if (line.equals("<ListOfAllocationProfiles>")) {
                    state = 1;
                }
                break;

            // reading profiles
            case 1:
                if (line.equals("<AllocationProfile>")) {
                    profile = new Profile();
                    state = 2;
                } else if (line.equals("</ListOfAllocationProfiles>")) {
                    state = 0;
                } else {
                    err(line);
                }
                break;

            // reading Profile
            case 2:
                if (line.startsWith("<name>")) {
                    profile.setName(getVal(line));
                } else if (line.startsWith("<type>")) {
                    int i = Integer.parseInt(getVal(line));
                    profile.setType(Type.get(i));
                } else if (line.startsWith("<ListOfAllocations")) {
                    state = 3;
                } else if (line.equals("</AllocationProfile>")) {
                    list.add(profile);
                    state = 1;
                } else {
                    err(line);
                }
                break;

            // reading list of allocations
            case 3:
                if (line.equals("<Allocation>")) {
                    alloc = new Allocation();
                    state = 4;
                } else if (line.equals("</ListOfAllocations>")) {
                    state = 2;
                } else {
                    err(line);
                }
                break;

            // reading Allocation
            case 4:
                if (line.startsWith("<acct>")) {
                    alloc.setAccount(getVal(line));
                } else if (line.startsWith("<amount>")) {
                    alloc.amount(getVal(line));
                } else if (line.startsWith("<posEff>")) {
                    // skip this
                } else if (line.equals("</Allocation>")) {
                    profile.addAllocation(alloc);
                    state = 3;
                } else {
                    err(line);
                }
                break;
            }
        }

        return list;
    }

    static List<Alias> getAliases(String xml) {
        try {
            return getAliases_(xml);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static List<Alias> getAliases_(String xml) throws IOException {
        ArrayList<Alias> list = new ArrayList<Alias>();

        Alias alias = null;

        BufferedReader reader = new BufferedReader(new StringReader(xml));
        String line;
        int state = 0; // 0=none; 1=list of aliases; 2=reading alias
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            switch (state) {
            // top of file
            case 0:
                if (line.equals("<ListOfAccountAliases>")) {
                    state = 1;
                }
                break;

            // reading aliases
            case 1:
                if (line.equals("<AccountAlias>")) {
                    alias = new Alias();
                    state = 2;
                } else if (line.equals("</ListOfAccountAliases>")) {
                    state = 0;
                } else {
                    err(line);
                }
                break;

            // reading Alias
            case 2:
                if (line.startsWith("<account>")) {
                    alias.setAccount(getVal(line));
                } else if (line.startsWith("<alias>")) {
                    alias.setAlias(getVal(line));
                } else if (line.equals("</AccountAlias>")) {
                    list.add(alias);
                    state = 1;
                } else {
                    err(line);
                }
                break;
            }
        }

        return list;
    }

    private static String getVal(String line) {
        int i1 = line.indexOf('>');
        int i2 = line.indexOf('<', 1);
        return line.substring(i1 + 1, i2);
    }

    private static void err(String line) {
        System.out.println("error " + line);
    }

    public static String getGroupsXml(List<Group> groups) {
        StringBuilder buf = new StringBuilder();
        buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        buf.append("<ListOfGroups>\n");
        for (Group group : groups) {
            buf.append("<Group>\n");
            buf.append(String.format("<name>%s</name>\n", group.getName()));
            buf.append(String.format("<defaultMethod>%s</defaultMethod>\n", group.getDefaultMethod()));
            buf.append("<ListOfAccts varName=\"list\"\n>");
            for (String acct : group.getAccounts()) {
                buf.append(String.format("<String>%s</String>\n", acct));
            }
            buf.append("</ListOfAccts>\n");
            buf.append("</Group>\n");
        }
        buf.append("</ListOfGroups>\n");
        return buf.toString();
    }

    public static String getProfilesXml(List<Profile> profiles) {
        StringBuilder buf = new StringBuilder();
        buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        buf.append("<ListOfProfiles>\n");
        for (Profile profile : profiles) {
            buf.append("<Profile>\n");
            buf.append(String.format("<name>%s</name>\n", profile.getName()));
            buf.append(String.format("<type>%s</type>\n", profile.getType().ordinal()));
            buf.append("<ListOfAllocations varName=\"listOfAllocations\">\n");
            for (Allocation alloc : profile.getAllocations()) {
                buf.append("<Allocation>\n");
                buf.append(String.format("<acct>%s</acct>\n", alloc.getAccount()));
                buf.append(String.format("<amount>%s</amount>\n", alloc.getAmount()));
                buf.append("</Allocation>\n");
            }
            buf.append("</ListOfAllocations>\n");
            buf.append("</Profile>\n");
        }
        buf.append("</ListOfProfiles>\n");
        return buf.toString();
    }
}
