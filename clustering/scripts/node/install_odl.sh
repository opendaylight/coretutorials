#!/bin/bash

# author__ = "Jan Medved"
# copyright__ = "Copyright(c) 2016, Cisco Systems, Inc."
# license__ = "Eclipse Public License (EPL) v1.0"
# email__ = "jmedved@cisco.com"

# Init our own program name
program_name=$0

function usage {
    echo ""
    echo "usage: $program_name [-h?an] [-d dest] [-i image] [-n host] [-p path] [-u user]"
    echo ""
    echo " -h|?        Print this message"
    echo " -d <dest>>  Desitnation folder where to install the ODL distro"
    echo " -i <image>  Name of the ODL distro to download"
    echo " -n <host>   IP address or name of the host from which to download"
    echo " -p <path>   Path to the distribution zip file. If <user> and <host>"
    echo "             are specified and <host> is not 'localhost, <path> is"
    echo "             the path on the remote host; otherwise, <path> is the"
    echo "             path to a (pre-downloaded) zip file on the local host."
    echo " -u <user>   User name for geÂtting the ODL distro from remote host"
    echo ""
}

# Initialize our own variables:

USER_NAME=""
REMOTE_PATH=""
HOST="localhost"
IMAGE=""
DESTINATION="odl"
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

while getopts "h?d:i:n:u:p:" opt; do
    case "$opt" in
    h|\?)
        usage
        exit 1
        ;;
    d)  DESTINATION=$OPTARG
        ;;
    i)  IMAGE=$OPTARG
        ;;
    n)  HOST=$OPTARG
        ;;
    n)  no_delete=true
        ;;
    u)  USER_NAME=$OPTARG
        ;;
    p)  REMOTE_PATH=$OPTARG
        ;;
    esac
done

if [ "$HOST" != "localhost" ]; then
    echo ""
    echo "ODL_INSTALL: Fetching ODL distro from '$HOST@$REMOTE_PATH/$IMAGE'..."
    echo ""

    COPY_ACTION=$(printf '%s %s@%s:%s/%s .' $IMG_FETCH_CMD $USER_NAME $HOST $REMOTE_PATH $IMAGE)
    RESULT=`$COPY_ACTION`
    if [ $? -ne 0 ]
    then
      echo "Could not fetch the ODL distribution file from $HOST@$REMOTE_PATH/$IMAGE" >&2
      exit 1
    fi
    IMAGE_WITH_PATH=./$IMAGE
else
    IMAGE_WITH_PATH=$(printf '%s/%s' $REMOTE_PATH $IMAGE)
    echo "IMAGE: $IMAGE"
fi

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
        exit 2
    fi
fi

$MK_DIR_CMD $DESTINATION
if [ $? -ne 0 ]
then
    echo "Could not create a folder for the new distro"
    $RM_FILE_CMD $IMAGE_WITH_PATH
    if [ "$DESTINATION_BACKUP" != "" ]; then
        $MV_CMD $DESTINATION_BACKUP $DESTINATION
    fi
    exit 3
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
    exit 4
fi

echo "ODL_INSTALL: Creating cluster configuration..."
echo ""

CURRENT_BIN_DIR=`dirname $program_name`
echo "CURRENT_BIN_DIR: $CURRENT_BIN_DIR"
CONTROLLER_BIN_DIR=$(printf '%s/%s/bin' $DESTINATION "${IMAGE%.*}")
$COPY_CMD $CURRENT_BIN_DIR/$CLUSTER_CFG_CMD $CONTROLLER_BIN_DIR
$CHANGE_DIR_CMD $CONTROLLER_BIN_DIR

./$CLUSTER_CFG_CMD 172.17.0.2 172.17.0.3 172.17.0.4
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
    exit 5
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
