package iotsimstream.examples;


import iotsimstream.BigDatacenter;
import iotsimstream.GraphAppEngine;
import iotsimstream.schedulingPolicies.Policy;
import iotsimstream.Properties;
import iotsimstream.edge.EdgeDataCenter;
import iotsimstream.edge.EdgeHost;
import iotsimstream.edge.EdgeProperties;
import iotsimstream.edge.EdgeVmAllocationPolicy;
import iotsimstream.edge.Location;
import iotsimstream.vmOffers.VMOffers;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.NetworkTopology;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

/**
 * This class contains the main method for execution of the IoTSim-Strem with App2.
 * Here, simulation parameters are defined. Parameters that are dynamic
 * are read from the properties file, whereas other parameters are hardcoded.
 * 
 * If you think that some of the hardcoded values should
 * be customizable, it can be added as a Property. In the Property code there
 * is comments on how to add new properties to the experiment.
 *
 */
public class App2Simulation {
    
    public App2Simulation(){
		
    }

    /**
     * Prints input parameters and execute the simulation a number of times,
     * as defined in the configuration.
     * 
     */
    public static void main(String[] args) {
        
        runSimulation();
    }

    public static void runSimulation() {

            try {
                    //App2 Example
                    //Simulate given stream graph application by setting dag.file property in simulation.properties file to the actual path of this application
                    Properties.DAG_FILE.setProperty(System.getProperty("user.dir") + File.separator + "Sample_Stream_Workflows/App2.xml");
                    
                    
                    //Get number of datacenters
                    int NumOfDatacenters = 0;
                    int NumOfEdgeDatacenters= 0;
                    
                  //Get number of datacenters
					if (Properties.DATACENTERS.getKey().contains("cloud")) {
						NumOfDatacenters = Integer.parseInt(Properties.DATACENTERS.getProperty());
     				}
                    if(EdgeProperties.EDGE_DATACENTER.getKey().contains("edge")) {
                    	NumOfEdgeDatacenters = Integer.parseInt(EdgeProperties.EDGE_DATACENTER.getProperty());
                   }
                    
                    //Print simulation configuration
                    Log.printLine("========== Simulation configuration ==========");
                    for (Properties property: Properties.values()){
                        if(property.getProperty(0)==null)
                            Log.printLine("= "+property+": "+property.getProperty());
                        else
                            for(int i=0;i<NumOfDatacenters;i++)
                                Log.printLine("= "+property+" (DC"+i+"): "+property.getProperty(i));
                    }
                    Log.printLine("==============================================");
                    Log.printLine("");
                
                    CloudSim.init(1,Calendar.getInstance(),false);

                    double intializationTime=0; //time required for intilization
                    
                    //Get the runtime of simulation
                    double requestedSimulationTime= Integer.parseInt(Properties.SIMULATION_TIME.getProperty()) + intializationTime; //simulation will run upto this time (i.e. stop simulation time)


                    ArrayList<BigDatacenter> listDatacenters=new ArrayList<BigDatacenter>();
                    ArrayList<EdgeDataCenter> listEdgeDatacenters=new ArrayList<EdgeDataCenter>();
                    ArrayList<Datacenter> totalDataCenter= new ArrayList<Datacenter>();
                    
                    long seedVmDelay= 1040529;
                    for(int i=0;i<NumOfDatacenters;i++)
                    {
                        BigDatacenter datacenter = createBigDatacenter(i, "Datacenter" + i, seedVmDelay);
                        listDatacenters.add(datacenter);
                        totalDataCenter.add(datacenter);
                    }
                    for(int i=0;i<NumOfEdgeDatacenters;i++)
                    {
                        EdgeDataCenter edgedatacenter = createEdgeDatacenter(i, "EdgeDatacenter" + i, seedVmDelay);
                        listEdgeDatacenters.add(edgedatacenter);
                        totalDataCenter.add(edgedatacenter);
                    }

                    //Create engine
                    GraphAppEngine engine = createGraphAppEngine(requestedSimulationTime);
                    

                    //Add ;inks between engine and datacenters and fill engress bandwidth and latency maps between datacenters
                    double engineBandwidth = Double.parseDouble(Properties.ENGINE_NETWORK_BANDWIDTH.getProperty()); //this bandwidth between engine and datacenters
                    double engineLatency = Double.parseDouble(Properties.ENGINE_NETWORK_LATENCY.getProperty()); //this latency between engine and datacenters
                    int startingSimCount=2;  // CloudSim starts counting for entity from 2, so the index of first datacenter is 2
                    double thisDatacenterEgressBw;
                    double thisDatacenterEgressLat;
                    for(int i=0;i<totalDataCenter.size();i++)
                    {
                        //Get datacenter    
                    	if(totalDataCenter.get(i).getName().contains("Edge")) {
                    		EdgeDataCenter datacenter=(EdgeDataCenter)totalDataCenter.get(i);
                    		
                    		//Add link between engine and this datacenter with engine network bandwidth and latency
                            NetworkTopology.addLink(engine.getId(),datacenter.getId(),engineBandwidth,engineLatency);

                            //Get egress bandwidth and latency of this datacenter with other datacenters (i.e egress network of this datacenter)
                            thisDatacenterEgressBw = Double.parseDouble(EdgeProperties.EDGE_EXTERNAL_BANDWIDTH.getProperty(totalDataCenter.get(i).getId()-startingSimCount)); //MBps //double bw = DSNetMatrix.getBandwidth(datacenterNumber, datacenterNumber);
                            thisDatacenterEgressLat = Double.parseDouble(EdgeProperties.EDGE_EXTERNAL_LATENCY.getProperty(totalDataCenter.get(i).getId()-startingSimCount)); //MBps //double bw = DSNetMatrix.getBandwidth(datacenterNumber, datacenterNumber);
 
                            //Fill egress bandwidth map and latency maps of this datacenter with other datacenters
                            for(int j=0;j<totalDataCenter.size();j++)
                            {
                                if(i==j)
                                  continue;

                                Datacenter otherDatacenter=totalDataCenter.get(j); 
                                datacenter.getDestDatacenterEgressBwMap().put(otherDatacenter.getId(), thisDatacenterEgressBw);
                                datacenter.getDestDatacenterEgressLatMap().put(otherDatacenter.getId(), thisDatacenterEgressLat);
                            }
                        }
                    	else {
							BigDatacenter datacenter = (BigDatacenter) totalDataCenter.get(i);
							// Add link between engine and this datacenter with engine network bandwidth and latency
							NetworkTopology.addLink(engine.getId(), datacenter.getId(), engineBandwidth, engineLatency);
							
							// Get egress bandwidth and latency of this datacenter with other datacenters(i.e egress network of this datacenter)
							thisDatacenterEgressBw = Double.parseDouble(Properties.EXTERNAL_BANDWIDTH.getProperty(totalDataCenter.get(i).getId() - startingSimCount)); // MBps //double bw =DSNetMatrix.getBandwidth(datacenterNumber,datacenterNumber);
							thisDatacenterEgressLat = Double.parseDouble(Properties.EXTERNAL_LATENCY.getProperty(totalDataCenter.get(i).getId() - startingSimCount)); // MBps double bw = DSNetMatrix.getBandwidth(datacenterNumber,datacenterNumber);

							// Fill egress bandwidth map and latency maps of this datacenter with other  datacenters
							for (int j = 0; j < totalDataCenter.size(); j++) {
								if (i == j)
									continue;
								
								Datacenter otherDatacenter =  totalDataCenter.get(j);
								datacenter.getDestDatacenterEgressBwMap().put(otherDatacenter.getId(),thisDatacenterEgressBw);
								datacenter.getDestDatacenterEgressLatMap().put(otherDatacenter.getId(),thisDatacenterEgressLat);
							}
                    		
                    	}
                    	
                    }
                    
                    CloudSim.startSimulation();
                    engine.printExecutionSummary();

                    CloudSim.stopSimulation();
                    
                    Log.printLine("");
                    Log.printLine("");
            } catch (Exception e) {
                    Log.printLine("Unwanted errors happen.");
                    e.printStackTrace();
            } finally {
                    CloudSim.stopSimulation();
            }
    }


    private static BigDatacenter createBigDatacenter(int datacenterNumber, String name, long seedVmDelayGenerator) throws Exception{
            int hosts = Integer.parseInt(Properties.HOSTS_PERDATACENTER.getProperty(datacenterNumber));
            int ram = Integer.parseInt(Properties.MEMORY_PERHOST.getProperty(datacenterNumber));
            int cores = Integer.parseInt(Properties.CORES_PERHOST.getProperty(datacenterNumber));
            int mips = Integer.parseInt(Properties.MIPS_PERCORE.getProperty(datacenterNumber));
            long storage = Long.parseLong(Properties.STORAGE_PERHOST.getProperty(datacenterNumber));
            double bw = Double.parseDouble(Properties.INTERNAL_BANDWIDTH.getProperty(datacenterNumber)); //MBps //double bw = DSNetMatrix.getBandwidth(datacenterNumber, datacenterNumber);
            double latency = Double.parseDouble(Properties.INTERNAL_LATENCY.getProperty(datacenterNumber)); //double latency = DSNetMatrix.getLatency(datacenterNumber, datacenterNumber);
            long creationVMDelay = Long.parseLong(Properties.VM_DELAY.getProperty(datacenterNumber));
            String offerName = Properties.VM_OFFERS.getProperty(datacenterNumber);
            
            VMOffers offers = null;
            try{				
                    Class<?> offerClass = Class.forName(offerName,true,VMOffers.class.getClassLoader());
                    offers = (VMOffers) offerClass.newInstance();
            } catch (Exception e){
                    e.printStackTrace();
                    return null;
            }

            List<Host> hostList = new ArrayList<Host>();
            for(int i=0;i<hosts;i++){
                    List<Pe> peList = new ArrayList<Pe>();
                    for(int j=0;j<cores;j++) 
                    {
                        peList.add(new Pe(j, new PeProvisionerSimple(mips)));
                    }

                    double totalHostBw=35000;
                    hostList.add(new Host(i,new RamProvisionerSimple(ram),new BwProvisionerSimple((long) totalHostBw),
                                                              storage,peList,new VmSchedulerSpaceShared(peList)));
            }

            DatacenterCharacteristics characteristics = new DatacenterCharacteristics("Xeon","Linux","Xen",hostList,10.0,0.0,0.00,0.00,0.00);

            //return new WorkflowDatacenter(name,characteristics,new VmAllocationPolicySimple(hostList),bw,latency,mips,delay,offers,seed2);
            return new BigDatacenter(name,characteristics,new VmAllocationPolicySimple(hostList),bw,latency,mips,creationVMDelay,offers,seedVmDelayGenerator);
    }
    
    private static EdgeDataCenter createEdgeDatacenter(int edgeDatacenterNumber, String name, long seedVmDelayGenerator)
			throws Exception {

		int hosts = Integer.parseInt(EdgeProperties.EDGE_HOST.getProperty(edgeDatacenterNumber));
		int ram = Integer.parseInt(EdgeProperties.EDGE_HOST_MEMORY.getProperty(edgeDatacenterNumber));
		int cores = Integer.parseInt(EdgeProperties.EDGE_HOST_CORES.getProperty(edgeDatacenterNumber));
		int mips = Integer.parseInt(EdgeProperties.EDGE_MIPS_PERCORE.getProperty(edgeDatacenterNumber));
		long storage = Long.parseLong(EdgeProperties.EDGE_HOST_STORAGE.getProperty(edgeDatacenterNumber));
		double bw = Double.parseDouble(Properties.INTERNAL_BANDWIDTH.getProperty(edgeDatacenterNumber));
		double latency = Double.parseDouble(Properties.INTERNAL_LATENCY.getProperty(edgeDatacenterNumber));
		long creationVMDelay = Long.parseLong(EdgeProperties.EDGE_VM_DELAY.getProperty(edgeDatacenterNumber));
		String offerName = EdgeProperties.VM_OFFERS.getProperty(edgeDatacenterNumber);
		String type = EdgeProperties.EDGE_HOST_TYPE.getProperty(edgeDatacenterNumber);
		Double locationX = Double.parseDouble(EdgeProperties.EDGE_HOST_LOCATION_X.getProperty(edgeDatacenterNumber));
		Double locationY = Double.parseDouble(EdgeProperties.EDGE_HOST_LOCATION_Y.getProperty(edgeDatacenterNumber));
		Double locationZ = Double.parseDouble(EdgeProperties.EDGE_HOST_LOCATION_Z.getProperty(edgeDatacenterNumber));
		Location loc = new Location(locationX, locationY, locationZ);

		VMOffers offers = null;
		try {
			Class<?> offerClass = Class.forName(offerName, true, VMOffers.class.getClassLoader());
			offers = (VMOffers) offerClass.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		List<EdgeHost> hostList = new ArrayList<EdgeHost>();
		for (int i = 0; i < hosts; i++) {
			List<Pe> peList = new ArrayList<Pe>();
			for (int j = 0; j < cores; j++) {
				peList.add(new Pe(j, new PeProvisionerSimple(mips)));
			}

			double totalHostBw = 10000;
			hostList.add(new EdgeHost(type, loc, i, new RamProvisionerSimple(ram),
					new BwProvisionerSimple((long) totalHostBw), storage, peList, new VmSchedulerTimeShared(peList)));
		}

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics("x86", "Linux", "Xen", hostList, 10.0,
				0.0, 0.05, 0.001, 0.00);

		return new EdgeDataCenter(name, characteristics, new EdgeVmAllocationPolicy(hostList), bw, latency, mips,
				creationVMDelay, offers, seedVmDelayGenerator);
	}

    private static GraphAppEngine createGraphAppEngine(double requestedST){
            String dagFile = Properties.DAG_FILE.getProperty();
            String className = Properties.SCHEDULING_POLICY.getProperty();
            
            Policy policy = null;
            
            try{		
                Class<?> policyClass = Class.forName(className);
                policy = (Policy) policyClass.newInstance();
                return new GraphAppEngine(dagFile,policy,requestedST);
            } catch (Exception e){
                    e.printStackTrace();
                    return null;
            }
    }
}
