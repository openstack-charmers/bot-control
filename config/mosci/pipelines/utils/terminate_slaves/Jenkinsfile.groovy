import hudson.model.*

echo "terminating: ${TERMINATE_SLAVES}"
for ( SLAVE in TERMINATE_SLAVES.split(',') ) {
    for (aSlave in hudson.model.Hudson.instance.nodes) { 
        if ( aSlave.toString().contains(SLAVE) ) {
            hudson.model.Hudson.instance.removeNode(aSlave)
        }  
    }
}
