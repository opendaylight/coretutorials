#!/bin/bash

# author__ = "Jan Medved"
# copyright__ = "Copyright(c) 2016, Cisco Systems, Inc."
# license__ = "Eclipse Public License (EPL) v1.0"
# email__ = "jmedved@cisco.com"

# Init our own program name
program_name=$0

function usage {
    echo ""
    echo 'usage: $program_name [-h?an] [-c "cluster-config"] [-d dest] [-i image] [-r remote-host] [-p path-to-distribution] [-u user]'
    echo ""
    echo " -h|?"
    echo "     Print this message"
    echo ' -c <"cluster-configuration">'
    echo "      IP addresses for all nodes in the cluster in form of a string "
    echo "      containing a comma/space delimited IP-address list"
    echo " -d <destination-folder>"
    echo "      Destination folder where to install the ODL distribution"
    echo "      Default: '/opt/odl'"
    echo " -i <distro-image>"
    echo "      Name of the ODL distro to download"
    echo " -p <path-to-distribution>"
    echo "      Path to the distribution zip file. If <user> and <host> are specified"
    echo "      and <host> is not 'localhost, <path> is the the path on the remote"
    echo "      host; otherwise, <path> is the  path to a (pre-downloaded) zip file"
    echo "      on the local host. Default: '.'"
    echo " -r <remote-host>"
    echo "      IP address or name of the host from which to download the distribution"
    echo "      Default: 'localhost'"
    echo " -u <user>"
    echo "      User name to login to <remote-host>"
    echo ""
    echo "Example:"
    echo "  $0 -d /opt/odl - i example-karaf-0.1.0-SNAPSHOT.zip -p . -c '172.17.0.2 172.17.0.3 172.17.0.4'"
    echo ""
}

# Initialize our own variables:

USER_NAME=""
REMOTE_PATH="."
HOST="localhost"
IMAGE=""
DESTINATION="/opt/odl"
DST_BACKUP=""
CURRENT_DIR=`pwd`

IMG_FETCH_CMD="scp"
RM_FILE_CMD="rm -f"
RM_DIR_CMD="rm -rf"
MK_DIR_CMD="mkdir"
MV_CMD="mv"
UNZIP_CMD="unzip"
COPY_CMD="cp"
CHANGE_DIR_CMD='cd'
CLUSTER_CFG_CMD="configure-cluster-ipdetect.sh"
CLUSTER_NODES=

while getopts "h?c:d:i:p:r:u:" opt; do
    case "$opt" in
    h|\?)
        usage
        exit 0
        ;;
    c)  IFS=', ' read -ra CLUSTER_NODES <<< "$OPTARG"
        ;;
    d)  DESTINATION=$OPTARG
        ;;
    i)  IMAGE=$OPTARG
        ;;
    n)  HOST=$OPTARG
        ;;
    u)  USER_NAME=$OPTARG
        ;;
    p)  REMOTE_PATH=$OPTARG
        ;;
    esac
done

if [ -z $CLUSTER_NODES ] ; then
    echo "Cluster configuration (option -c) must be specified"
    exit 1
fi

if [ "$HOST" != "localhost" ]; then
    echo ""
    echo "ODL_INSTALL: Fetching ODL distro from '$HOST@$REMOTE_PATH/$IMAGE'..."
    echo ""

    COPY_ACTION=$(printf '%s %s@%s:%s/%s .' $IMG_FETCH_CMD $USER_NAME $HOST $REMOTE_PATH $IMAGE)
    RESULT=`$COPY_ACTION`
    if [ $? -ne 0 ]
    then
      echo "Could not fetch the ODL distribution file from $HOST@$REMOTE_PATH/$IMAGE" >&2
      exit 2
    fi
    IMAGE_WITH_PATH=./$IMAGE
else
    IMAGE_WITH_PATH=$(printf '%s/%s' $REMOTE_PATH $IMAGE)
    echo "IMAGE: $IMAGE"
fi

# If there is an installation present in a folder with the same name
# as the destination folder, , move it to backup

DESTINATION_BACKUP=""
if [ -d "$DESTINATION" ]; then
    echo "Backing up existing installation to '$DESTINATION.backup'"
    echo ""

    DESTINATION_BACKUP=$(printf '%s.backup' $DESTINATION)
    $MV_CMD $DESTINATION $DESTINATION_BACKUP
    if [ $? -ne 0 ]
    then
        echo "Could not back up the existing distro"
        if [ "$HOST" != "localhost" ]; then
            $RM_FILE_CMD $IMAGE_WITH_PATH
        fi
        exit 3
    fi
fi

# Create a folder for the installation

$MK_DIR_CMD $DESTINATION
if [ $? -ne 0 ]
then
    echo "Could not create a folder for the new distro"
    $RM_FILE_CMD $IMAGE_WITH_PATH
    if [ "$DESTINATION_BACKUP" != "" ]; then
        $MV_CMD $DESTINATION_BACKUP $DESTINATION
    fi
    exit 4
fi

echo "ODL_INSTALL: Extracting the downloaded distribution to '$DESTINATION'"
echo ""
$UNZIP_CMD $IMAGE_WITH_PATH -d $DESTINATION >/dev/null
if [ $? -ne 0 ]
then
    echo "Could not unzip the distro file"
    if [ "$HOST" != "localhost" ]; then
        $RM_FILE_CMD $IMAGE_WITH_PATH
    fi
    $RM_DIR_CMD $DESTINATION
    if [ "$DESTINATION_BACKUP" != "" ]; then
        $MV_CMD $DESTINATION_BACKUP $DESTINATION
    fi
    exit 5
fi

echo "ODL_INSTALL: Creating cluster configuration..."
echo ""

# Move the cluster configuration script into place in the karaf bin folder 
# of the newly extracted distribution

CURRENT_BIN_DIR=`dirname $program_name`
CONTROLLER_BIN_DIR=$(printf '%s/%s/bin' $DESTINATION "${IMAGE%.*}")
$COPY_CMD $CURRENT_BIN_DIR/$CLUSTER_CFG_CMD $CONTROLLER_BIN_DIR

# Change execution folder to the karaf bin folder; this is expected by
# the cluster configuration script

$CHANGE_DIR_CMD $CONTROLLER_BIN_DIR

# Execute the cluster configuration script

./$CLUSTER_CFG_CMD ${CLUSTER_NODES[@]}
if [ $? -ne 0 ]
then
    echo "Could not configure clustering"
    $CHANGE_DIR_CMD $CURRENT_DIR
    if [ "$HOST" != "localhost" ]; then
        $RM_FILE_CMD $IMAGE_WITH_PATH
    fi
    $RM_DIR_CMD $DESTINATION
    if [ "$DESTINATION_BACKUP" != "" ]; then
        $MV_CMD $DESTINATION_BACKUP $DESTINATION
    fi
    exit 6
fi

$CHANGE_DIR_CMD $CURRENT_DIR

echo ""
echo "ODL_INSTALL: Cleaning up temporary files and folders"
if [ "$HOST" != "localhost" ]; then
    $RM_FILE_CMD $IMAGE_WITH_PATH
fi
$RM_DIR_CMD $DESTINATION_BACKUP

echo ""
exit 0
