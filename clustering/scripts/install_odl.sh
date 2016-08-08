#!/bin/bash

# author__ = "Jan Medved"
# copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
# license__ = "New-style BSD"
# email__ = "jmedved@cisco.com"

# Init our own program name
program_name=$0

function usage {
    echo "usage: $program_name [-h?an] [-i instances] [-c cycles] [-f flows] [-t threads] [-o odl_host] [-p odl_port]"
    echo " -h|?           Print this message"
    echo " -d <dest>>     Desitnation folder where to install the ODL distro"
    echo " -i <image>     Name of the ODL distro to download"
    echo " -n <host-name> IP address or name of the host from which to download"
    echo " -u <user>      User name for getting the ODL distro from remote host"
    echo " -p <path>      Path to the ODL distro on the host from which to download"
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

echo "Backing up existing installation to '$DESTINATION.backup'"
echo ""

DESTINATION_BACKUP=$(printf '%s.backup' $DESTINATION)
if [ -d "$DESTINATION" ]; then
  $MV_CMD $DESTINATION $DESTINATION_BACKUP
  if [ $? -ne 0 ]
  then
    echo "Could not back up the existing distro"
    $RM_FILE_CMD $IMAGE
    exit 2
  fi
fi

$MK_DIR_CMD $DESTINATION
if [ $? -ne 0 ]
then
  echo "Could not create a folder for the new distro"
  $RM_FILE_CMD $IMAGE
  $MV_CMD $DESTINATION_BACKUP $DESTINATION
  exit 3
fi

echo "ODL_INSTALL: Extracting the downloaded distribution to '$DESTINATION'"
echo ""
$UNZIP_CMD $IMAGE -d $DESTINATION >/dev/null
if [ $? -ne 0 ]
then
  echo "Could not unzip the distro file"
  $RM_FILE_CMD $IMAGE
  $RMDIR_CMD $DESTINATION
  $MV_CMD $DESTINATION_BACKUP $DESTINATION
  exit 4
fi

echo "ODL_INSTALL: Creating cluster configuration..."
echo ""

CONTROLLER_BIN_DIR=$(printf '%s/%s/bin' $DESTINATION "${IMAGE%.*}")
$COPY_CMD "scripts/configure_cluster.sh" $CONTROLLER_BIN_DIR
$CHANGE_DIR_CMD $CONTROLLER_BIN_DIR

./configure_cluster.sh 172.17.0.2 172.17.0.3 172.17.0.4
if [ $? -ne 0 ]
then
  echo "Could not configure clustering"
  $CHANGE_DIR_CMD $CURRENT_DIR
  $RM_FILE_CMD $IMAGE
  $RMDIR_CMD $DESTINATION
  $MV_CMD $DESTINATION_BACKUP $DESTINATION
  exit 5
fi
$CHANGE_DIR_CMD $CURRENT_DIR

echo ""
echo "ODL_INSTALL: Cleaning up temporary files and folders"
$RM_FILE_CMD $IMAGE
$RM_DIR_CMD $DESTINATION_BACKUP

echo ""
exit 0
