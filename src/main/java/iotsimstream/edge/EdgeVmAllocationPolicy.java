package iotsimstream.edge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;

/**
*  This class represents the provisioning policy of edge hosts to virtual machines in a edge datacenter. It allocates hosts for placing VMs. 
*
* @author Gursharn Soni
*/
public class EdgeVmAllocationPolicy extends VmAllocationPolicy {

	/** The map between each VM and its allocated host.
         * The map key is a VM UID and the value is the allocated host for that VM. */
	private Map<String, EdgeHost> vmTable;

	/** The map between each VM and the number of Pes used. 
         * The map key is a VM UID and the value is the number of used Pes for that VM. */
	private Map<String, Integer> usedPes;

	/** The number of free Pes for each host from {@link #getHostList() }. */
	private List<Integer> freePes;

	/**
	 * Creates a new VmAllocationPolicySimple object.
	 * 
	 * @param list the list of hosts
	 * @pre $none
	 * @post $none
	 */
	public EdgeVmAllocationPolicy(List<EdgeHost> list) {
		super(list);
		this.hostList = list;

		setFreePes(new ArrayList<Integer>());
		for (EdgeHost host : getHostList()) {
			getFreePes().add(host.getNumberOfPes());

		}

		setVmTable(new HashMap<String, EdgeHost>());
		setUsedPes(new HashMap<String, Integer>());
	}

	/**
	 * Allocates the host with less PEs in use for a given VM.
	 * 
	 * @param vm {@inheritDoc}
	 * @return {@inheritDoc}
	 * @pre $none
	 * @post $none
	 */
	@Override
	public boolean allocateHostForVm(Vm vm) {
		int requiredPes = vm.getNumberOfPes();
		boolean result = false;
		int tries = 0;
		List<Integer> freePesTmp = new ArrayList<Integer>();
		for (Integer freePes : getFreePes()) {
			freePesTmp.add(freePes);
		}

		if (!getVmTable().containsKey(vm.getUid())) { // if this vm was not created
			do {// we still trying until we find a host or until we try all of them
				int moreFree = Integer.MIN_VALUE;
				int idx = -1;

				// we want the host with less pes in use
				for (int i = 0; i < freePesTmp.size(); i++) {
					if (freePesTmp.get(i) > moreFree) {
						moreFree = freePesTmp.get(i);
						idx = i;
					}
				}

				EdgeHost host = getHostList().get(idx);
				result = host.vmCreate(vm);

				if (result) { // if vm were succesfully created in the host
					getVmTable().put(vm.getUid(), host);
					getUsedPes().put(vm.getUid(), requiredPes);
					getFreePes().set(idx, getFreePes().get(idx) - requiredPes);
					result = true;
					break;
				} else {
					freePesTmp.set(idx, Integer.MIN_VALUE);
				}
				tries++;
			} while (!result && tries < getFreePes().size());

		}

		return result;
	}

	@Override
	public void deallocateHostForVm(Vm vm) {
		Host host = getVmTable().remove(vm.getUid());
		int idx = getHostList().indexOf(host);
		int pes = getUsedPes().remove(vm.getUid());
		if (host != null) {
			host.vmDestroy(vm);
			getFreePes().set(idx, getFreePes().get(idx) + pes);
		}
	}

	@Override
	public EdgeHost getHost(Vm vm) {
		return getVmTable().get(vm.getUid());
	}

	@Override
	public EdgeHost getHost(int vmId, int userId) {
		return getVmTable().get(Vm.getUid(userId, vmId));
	}

	/**
	 * Gets the vm table.
	 * 
	 * @return the vm table
	 */
	public Map<String, EdgeHost> getVmTable() {
		return vmTable;
	}

	/**
	 * Sets the vm table.
	 * 
	 * @param vmTable the vm table
	 */
	protected void setVmTable(Map<String, EdgeHost> vmTable) {
		this.vmTable = vmTable;
	}

	/**
	 * Gets the used pes.
	 * 
	 * @return the used pes
	 */
	protected Map<String, Integer> getUsedPes() {
		return usedPes;
	}

	/**
	 * Sets the used pes.
	 * 
	 * @param usedPes the used pes
	 */
	protected void setUsedPes(Map<String, Integer> usedPes) {
		this.usedPes = usedPes;
	}

	/**
	 * Gets the free pes.
	 * 
	 * @return the free pes
	 */
	protected List<Integer> getFreePes() {
		return freePes;
	}

	/**
	 * Sets the free pes.
	 * 
	 * @param freePes the new free pes
	 */
	protected void setFreePes(List<Integer> freePes) {
		this.freePes = freePes;
	}

	@Override
	public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean allocateHostForVm(Vm vm,  Host host) {
		/*if (host.vmCreate(vm)) { // if vm has been succesfully created in the host
			getVmTable().put(vm.getUid(), host);

			int requiredPes = vm.getNumberOfPes();
			int idx = getHostList().indexOf(host);
			getUsedPes().put(vm.getUid(), requiredPes);
			getFreePes().set(idx, getFreePes().get(idx) - requiredPes);

			Log.formatLine(
					"%.2f: VM #" + vm.getId() + " has been allocated to the host #" + host.getId(),
					CloudSim.clock());
			return true;
		}*/

		return false;
	}
	private List<EdgeHost> hostList;
	
	public List<EdgeHost> getHostList() {
		return  hostList;
	}

}
