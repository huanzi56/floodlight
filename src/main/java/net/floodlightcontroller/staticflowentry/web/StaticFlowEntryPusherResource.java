/**
*    Copyright 2011, Big Switch Networks, Inc.
*    Originally created by David Erickson, Stanford University
*
*    Licensed under the Apache License, Version 2.0 (the "License"); you may
*    not use this file except in compliance with the License. You may obtain
*    a copy of the License at
*
*         http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
*    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
*    License for the specific language governing permissions and limitations
*    under the License.
**/

package net.floodlightcontroller.staticflowentry.web;

import java.io.IOException;
import java.util.Map;

import org.restlet.resource.Delete;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.annotations.LogMessageCategory;
import net.floodlightcontroller.core.annotations.LogMessageDoc;
import net.floodlightcontroller.staticflowentry.StaticFlowEntries;
import net.floodlightcontroller.staticflowentry.StaticFlowEntryPusher;
import net.floodlightcontroller.storage.IStorageSourceService;

/**
 * Pushes a static flow entry to the storage source
 * @author alexreimers
 *
 */
@LogMessageCategory("Static Flow Pusher")
public class StaticFlowEntryPusherResource extends ServerResource {
    protected static Logger log = LoggerFactory.getLogger(StaticFlowEntryPusherResource.class);

	public static boolean ip6 = false;
	public static boolean ip4 = false;
	
	/**
	 * Validates if all the mandatory fields are set properly while adding an IPv6 flow
	 * @param Map containing the fields of the flow
	 * @return state indicating whether a flow is valid or not
	 */
    private int checkMatch(Map<String, Object> rows) {
    	
 //sanjivini   
    	//Initializing flags
    	int state = 0;
    	boolean dl_type = false;
    	boolean nw_proto = false;
    	boolean nw_layer = false;
    	boolean icmp6_type = false;
    	boolean icmp6_code = false;
    	boolean nd_target = false;
    	boolean nd_sll = false;
    	boolean nd_tll = false;
    	
    	String eth_type = null;
    	String nw_protocol = null;
    	int icmp_type = -1;
    	
    	//Determine the dl_type if set
    	if (rows.containsKey(StaticFlowEntryPusher.COLUMN_DL_TYPE)) {
    		dl_type = true;
    		eth_type = (String) rows.get(StaticFlowEntryPusher.COLUMN_DL_TYPE);
    		if (eth_type.equalsIgnoreCase("0x86dd") || eth_type.equals("34525")) {
    			ip6 = true;
    		}
    		else if (eth_type.equalsIgnoreCase("0x800") || eth_type.equals("2048")||
					eth_type.equalsIgnoreCase("0x806") || eth_type.equals("2054")) {
    			ip4 = true;
    		}
    		else {
    			state = 2;
    			return state;
    		}
    	}
    	if (rows.containsKey(StaticFlowEntryPusher.COLUMN_NW_DST) ||
    			rows.containsKey(StaticFlowEntryPusher.COLUMN_NW_SRC)) {
    		nw_layer = true;
    		ip4 = true;
    	}
    	if (rows.containsKey(StaticFlowEntryPusher.COLUMN_ICMP_CODE) ||
    			rows.containsKey(StaticFlowEntryPusher.COLUMN_ICMP_TYPE) ||
    			rows.containsKey(StaticFlowEntryPusher.COLUMN_ARP_DHA) ||
    			rows.containsKey(StaticFlowEntryPusher.COLUMN_ARP_SHA) ||
    			rows.containsKey(StaticFlowEntryPusher.COLUMN_ARP_SPA) ||
    			rows.containsKey(StaticFlowEntryPusher.COLUMN_ARP_DPA) ||
    			rows.containsKey(StaticFlowEntryPusher.COLUMN_ARP_OPCODE)) {
    		ip4 = true;
    	}
    	if (rows.containsKey(StaticFlowEntryPusher.COLUMN_IPV6_FLOW_LABEL) || 
    			rows.containsKey(StaticFlowEntryPusher.COLUMN_NW6_SRC) ||
    			rows.containsKey(StaticFlowEntryPusher.COLUMN_NW6_DST)
    			) {
    		nw_layer = true;
    		ip6 = true;
    	}
    	if (rows.containsKey(StaticFlowEntryPusher.COLUMN_NW_PROTO)) {
    		nw_proto = true;
    		nw_protocol = (String) rows.get(StaticFlowEntryPusher.COLUMN_NW_PROTO);
    	}
    	if (rows.containsKey(StaticFlowEntryPusher.COLUMN_ICMP6_CODE)) {
    		icmp6_code = true;
    		ip6 = true;
    	}
    	if (rows.containsKey(StaticFlowEntryPusher.COLUMN_ICMP6_TYPE)) {
    		icmp6_type = true;
    		ip6 = true;
    		icmp_type = Integer.parseInt((String) rows.get(StaticFlowEntryPusher.COLUMN_ICMP6_TYPE));
    	}
    	if (rows.containsKey(StaticFlowEntryPusher.COLUMN_ND_SLL)) {
    		nd_sll = true;
    		ip6 = true;
    	}
    	if (rows.containsKey(StaticFlowEntryPusher.COLUMN_ND_TLL)) {
    		nd_tll = true;
    		ip6 = true;
    	}
    	if (rows.containsKey(StaticFlowEntryPusher.COLUMN_ND_TARGET)) {
    		nd_target = true;
    		ip6 = true;
    	}    	
    	
    	if (nw_layer == true || nw_proto == true) {
    		if (dl_type == true) {
    			if (ip4 == false || ip6 == false) {
    				//invalid dl_type
    				state = 2;
    				return state;
    			}
    		}
    		else {
    			//dl_type not set
    			state = 1;
    			return state;
    		}
    	}
    	if (icmp6_type == true || icmp6_code == true ) {
    		if (nw_proto == true) {
    			if (!(nw_protocol.equals("58") || nw_protocol.equalsIgnoreCase("0x3A"))) {
    				//invalid nw_proto
    				state = 4;
    				return state;
    			}
    		}
    		else {
    			//nw_proto not set
    			state = 3;
    			return state;
    		}
    	}
    	
    	if (nd_sll == true || nd_tll == true || nd_target == true) {
    		if (icmp6_type == true) {
    			//icmp_type must be set to 135/136 to set ipv6_nd_target		
				if (nd_target == true) {
					if (!(icmp_type == 135 || icmp_type == 136)) {
						//invalid icmp6_type
						state = 6;
						return state;
					}
				}
				//icmp_type must be set to 136 to set ipv6_nd_tll
				else if (nd_tll == true) {
					if (!(icmp_type == 136)) {
						//invalid icmp6_type
						state = 6;
						return state;
					}
				}
				//icmp_type must be set to 135 to set ipv6_nd_sll
				else if (nd_sll == true) {
					if (!(icmp_type == 135)) {
						//invalid icmp6_type
						state = 6;
						return state;
					}
				}
    		}
    		else {
    			//icmp6_type not set
    			state = 5;
    			return state;
    		}
    	}
    	
    	if (ip4 == true && ip6 == true) {
    		//ipv4 & ipv6 conflict
    		state = 7;
    		return state;
    	}
    	
    	return state;
//sanjivini   	    	
    	
    	/*
        boolean matchEther = false;
        String val = (String) rows.get(StaticFlowEntryPusher.COLUMN_DL_TYPE);
        if (val != null) {
            int type = 0;
            // check both hex and decimal
            if (val.startsWith("0x")) {
                type = Integer.parseInt(val.substring(2), 16);
            } else {
                try {
                    type = Integer.parseInt(val);
                } catch (NumberFormatException e) { /* fail silently */ /*}
            }
            if ((type == 2048)|| (type == 34525))  matchEther = true;
        }

        if ((rows.containsKey(StaticFlowEntryPusher.COLUMN_NW_DST) ||
                rows.containsKey(StaticFlowEntryPusher.COLUMN_NW_SRC) ||
                rows.containsKey(StaticFlowEntryPusher.COLUMN_NW6_SRC) ||
                rows.containsKey(StaticFlowEntryPusher.COLUMN_NW6_DST) ||
                rows.containsKey(StaticFlowEntryPusher.COLUMN_NW_PROTO) ||
                rows.containsKey(StaticFlowEntryPusher.COLUMN_NW_TOS)) &&
                (matchEther == false))
            return false;

        return true;
        */
    }

    /**
     * Takes a Static Flow Pusher string in JSON format and parses it into
     * our database schema then pushes it to the database.
     * @param fmJson The Static Flow Pusher entry in JSON format.
     * @return A string status message
     */
    @Post
    @LogMessageDoc(level="ERROR",
        message="Error parsing push flow mod request: {request}",
        explanation="An invalid request was sent to static flow pusher",
        recommendation="Fix the format of the static flow mod request")
    public String store(String fmJson) {
        IStorageSourceService storageSource =
                (IStorageSourceService)getContext().getAttributes().
                    get(IStorageSourceService.class.getCanonicalName());

        Map<String, Object> rowValues;
        try {
            rowValues = StaticFlowEntries.jsonToStorageEntry(fmJson);
            String status = null;
            int state = checkMatch(rowValues);
            if (state == 1) {
                status = "Warning! Must specify eth_type of IPv4/IPv6 to " +
                        "match on IPv4/IPv6 fields! The flow has been discarded.";
                log.error(status);
            } else if (state == 2) {
            	status = "Warning! eth_type not recognized! The flow has been discarded.";
                log.error(status);
            } else if (state == 3) {
            	status = "Warning! Must specify ip_proto to match! The flow has been discarded.";
                log.error(status);
            } else if (state == 4) {
            	status = "Warning! ip_proto invalid! The flow has been discarded.";
                log.error(status);
            } else if (state == 5) {
            	status = "Warning! Must specify icmp6_type to match! The flow has been discarded.";
                log.error(status);
            } else if (state == 6) {
            	status = "Warning! icmp6_type invalid! The flow has been discarded.";
                log.error(status);
            } else if (state == 7) {
            	status = "Warning! IPv4 & IPv6 fields cannot be specified in the same flow! The flow has been discarded.";
                log.error(status);
            } else if (state == 0) {
                status = "Entry pushed";
            }
            storageSource.insertRowAsync(StaticFlowEntryPusher.TABLE_NAME, rowValues);
            return ("{\"status\" : \"" + status + "\"}");
        } catch (IOException e) {
            log.error("Error parsing push flow mod request: " + fmJson, e);
            return "{\"status\" : \"Error! Could not parse flod mod, see log for details.\"}";
        }
        catch (Exception e) {
            log.error("Error parsing push flow mod request: " + fmJson, e);
            return "{\"status\" : \"Error! Check the fields specified for the flow.Make sure IPv4 fields are not mixed with IPv6 fields or all "
            		+ "mandatory fields are specified.\"}";
        }
    }

    @Delete
    @LogMessageDoc(level="ERROR",
        message="Error deleting flow mod request: {request}",
        explanation="An invalid delete request was sent to static flow pusher",
        recommendation="Fix the format of the static flow mod request")
    public String del(String fmJson) {
        IStorageSourceService storageSource =
                (IStorageSourceService)getContext().getAttributes().
                    get(IStorageSourceService.class.getCanonicalName());
        String fmName = null;
        if (fmJson == null) {
            return "{\"status\" : \"Error! No data posted.\"}";
        }
        try {
            fmName = StaticFlowEntries.getEntryNameFromJson(fmJson);
            if (fmName == null) {
                return "{\"status\" : \"Error deleting entry, no name provided\"}";
            }
        } catch (IOException e) {
            log.error("Error deleting flow mod request: " + fmJson, e);
            return "{\"status\" : \"Error deleting entry, see log for details\"}";
        }

        storageSource.deleteRowAsync(StaticFlowEntryPusher.TABLE_NAME, fmName);
        return "{\"status\" : \"Entry " + fmName + " deleted\"}";
    }
}
