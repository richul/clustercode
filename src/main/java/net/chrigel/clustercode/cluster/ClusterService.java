package net.chrigel.clustercode.cluster;

import net.chrigel.clustercode.task.Media;

import java.util.Optional;

public interface ClusterService {

    /**
     * Joins the cluster. If this Java process is the first member, it will create a new cluster. If a cluster cannot be
     * created, it will downgrade to a single-node cluster.
     */
    void joinCluster();

    /**
     * Leaves the cluster, if it was connected.
     */
    void leaveCluster();

    /**
     * Removes the currently active task from the cluster, if there was one set.
     */
    void removeTask();

    /**
     * Gets the task that is or was scheduled for this node. This method is useful after an application crash where the
     * cluster knows which task was scheduled for this specific node. So this method returns the candidate previously
     * scheduled for this node, otherwise empty.
     *
     * @return an optional describing the task.
     */
    Optional<Media> getTask();

    /**
     * Sets the task which is being executed by this Java process. Replaces the old task if present, only one task can
     * be active. Tasks which are long in the cluster than {@link ClusterSettings#getTaskTimeout()} are being removed.
     * This method does nothing if not connected to the cluster.
     *
     * @param candidate the task, not null.
     */
    void setTask(Media candidate);

    /**
     * Returns true if the candidate is known across the cluster. If this Java process is the only member or not at all
     * in the cluster, it returns false.
     *
     * @param candidate the candidate, not null.
     * @return true if queued.
     */
    boolean isQueuedInCluster(Media candidate);
}