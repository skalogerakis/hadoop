package org.apache.hadoop.hdfs.server.blockmanagement;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.StorageType;
import org.apache.hadoop.hdfs.AddBlockFlag;
import org.apache.hadoop.hdfs.protocol.BlockStoragePolicy;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.net.NetworkTopology;
import org.apache.hadoop.net.Node;

import java.util.*;

public class BlockPlacementPolicyAffinity extends BlockPlacementPolicy{

    protected Configuration conf;
    protected DatanodeManager datanodeManager;
    protected NetworkTopology clusterMap;
    private FSClusterStats stats;
    protected Map<String, DatanodeStorageInfo[]> clientToDataNodesMap;

    protected BlockPlacementPolicyAffinity() {
        clientToDataNodesMap = new HashMap<>();
    }

    @Override
    protected void initialize(Configuration conf,
                              FSClusterStats stats,
                              NetworkTopology clusterMap,
                              Host2NodesMap host2datanodeMap) {
        this.stats = stats;
        this.conf = conf;
        this.datanodeManager = datanodeManager;
        this.clusterMap = datanodeManager.getNetworkTopology();
    }

    @Override
    public DatanodeStorageInfo[] chooseTarget(String srcPath,
                                              int numOfReplicas,
                                              Node writer,
                                              List<DatanodeStorageInfo> chosen,
                                              boolean returnChosenNodes,
                                              Set<Node> excludedNodes,
                                              long blocksize,
                                              BlockStoragePolicy storagePolicy,
                                              EnumSet<AddBlockFlag> flags) {

        if (numOfReplicas <= 0 || clusterMap.getNumOfLeaves() == 0) {
            return DatanodeStorageInfo.EMPTY_ARRAY;
        }

        // Retrieve the client's IP address from the writer node
        String clientIP = ((DatanodeDescriptor) writer).getIpAddr();
        LOG.info("Client IP = {}", clientIP);


        // Check if the client's targets have already been determined
        if (clientToDataNodesMap.containsKey(clientIP)) {
            return clientToDataNodesMap.get(clientIP);
//            return getPipeline(writer, clientToDataNodesMap.get(clientIP));
        }


        // Get a list of eligible datanodes excluding the local machine
        List<DatanodeStorageInfo> candidateNodes = new ArrayList<>();
        for (DatanodeStorageInfo storageInfo : chosen) {
            DatanodeDescriptor datanode = storageInfo.getDatanodeDescriptor();
            if (!datanode.getIpAddr().equals(clientIP)) {
                candidateNodes.add(storageInfo);
            }
        }

        // Choose target datanodes from the candidate nodes
        int numOfTargets = Math.min(numOfReplicas, candidateNodes.size());
        DatanodeStorageInfo[] targets = new DatanodeStorageInfo[numOfTargets];
        for (int i = 0; i < numOfTargets; i++) {
            targets[i] = candidateNodes.get(i);
        }

        // Cache the client's targets for future reference
        clientToDataNodesMap.put(clientIP, targets);


        // sorting nodes to form a pipeline
//        return getPipeline(
//                (writer != null && writer instanceof DatanodeDescriptor) ? writer : null,
//                targets);
        return targets;
    }

    @Override
    public BlockPlacementStatus verifyBlockPlacement(DatanodeInfo[] locs,
                                                     int numOfReplicas) {

        if (locs == null) locs = DatanodeDescriptor.EMPTY_ARRAY;

        if (!clusterMap.hasClusterEverBeenMultiRack()) {
            // only one rack
            return new BlockPlacementStatusDefault(1, 1, 1);
        }

        // Count the different machines
//        Map<String, Integer> ipCountMap = new HashMap<>();
//        for (DatanodeInfo loc : locs) {
//            String ipAddr = loc.getIpAddr();
//            ipCountMap.put(ipAddr, ipCountMap.getOrDefault(ipAddr, 0) + 1);
//        }
//
//        int numOfTargets = Math.min(numOfReplicas, ipCountMap.size());
//
//        if (locs.length == numOfTargets) {
//            return new BlockPlacementStatusDefault(0, 2,0);
//        }

        return new BlockPlacementStatusDefault(1, 1,1);


    }

    @Override
    public List<DatanodeStorageInfo> chooseReplicasToDelete(Collection<DatanodeStorageInfo> availableReplicas,
                                                            Collection<DatanodeStorageInfo> delCandidates,
                                                            int expectedNumOfReplicas,
                                                            List<StorageType> excessTypes,
                                                            DatanodeDescriptor addedNode,
                                                            DatanodeDescriptor delNodeHint) {
        //TODO
        return null;
    }



    @Override
    public boolean isMovable(Collection<DatanodeInfo> candidates, DatanodeInfo source, DatanodeInfo target) {
        return false;
    }

    /**
     * Return a pipeline of nodes.
     * The pipeline is formed finding a shortest path that
     * starts from the writer and traverses all <i>nodes</i>
     * This is basically a traveling salesman problem.
     */
    private DatanodeStorageInfo[] getPipeline(Node writer,
                                              DatanodeStorageInfo[] storages) {
        if (storages.length == 0) {
            return storages;
        }

        synchronized(clusterMap) {
            int index=0;
            if (writer == null || !clusterMap.contains(writer)) {
                writer = storages[0].getDatanodeDescriptor();
            }
            for(; index < storages.length; index++) {
                DatanodeStorageInfo shortestStorage = storages[index];
                int shortestDistance = clusterMap.getDistance(writer,
                        shortestStorage.getDatanodeDescriptor());
                int shortestIndex = index;
                for(int i = index + 1; i < storages.length; i++) {
                    int currentDistance = clusterMap.getDistance(writer,
                            storages[i].getDatanodeDescriptor());
                    if (shortestDistance>currentDistance) {
                        shortestDistance = currentDistance;
                        shortestStorage = storages[i];
                        shortestIndex = i;
                    }
                }
                //switch position index & shortestIndex
                if (index != shortestIndex) {
                    storages[shortestIndex] = storages[index];
                    storages[index] = shortestStorage;
                }
                writer = shortestStorage.getDatanodeDescriptor();
            }
        }
        return storages;
    }
}
