package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.Container;
import com.github.murataykanat.toybox.repositories.ContainersRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class ContainerUtils {
    private static final Log _logger = LogFactory.getLog(ContainerUtils.class);
    private static ContainerUtils containerUtils;

    private ContainerUtils(){}

    public static ContainerUtils getInstance(){
        if(containerUtils != null){
            return containerUtils;
        }

        containerUtils = new ContainerUtils();
        return containerUtils;
    }

    @LogEntryExitExecutionTime
    public Container getContainer(ContainersRepository containersRepository, String containerId) throws Exception {
        List<Container> containersById = containersRepository.getContainersById(containerId);
        if(!containersById.isEmpty()){
            if(containersById.size() == 1){
                return containersById.get(0);
            }
            else{
                throw new Exception("There are multiple containers with ID '" + containerId + "'!");
            }
        }
        else{
            throw new Exception("There is no container with ID '" + containerId + "'!");
        }
    }
}
