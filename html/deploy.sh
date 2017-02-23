#!/usr/bin/env bash

# Deploy
# Nginx, Node.js 
# to Ubuntu 12.04 server 

# To improve ssh performance, consider reusing connections:
#
# Host *
# ControlMaster auto
# ControlPath /tmp/%r@%h:%p

# Configuration
SERVER='176.34.11.40'
DEPLOY_TO='/var/www/weik.metaisle.com'
EXCLUDE='*.swp .git/ db/sphinx/ tmp/ log/'
DRY_RUN=false

SSH="ssh -i $SERVER.pem $SERVER"


# Map the excluded files to rsync's options
function excludes {
  EXCLUDES=''
  for i in $(echo $EXCLUDE | tr " " "\n")
  do
EXCLUDES="$EXCLUDES --exclude $i"
  done
}


# Run rsync
function upload_files {
  excludes

  CMD="rsync -avz $EXCLUDES"
  if $DRY_RUN ; then
CMD="$CMD --dry-run"
  fi

CMD="$CMD ./ $SERVER:$DEPLOY_TO"
  echo $CMD
  $CMD
}

function install_nginx (){
	$SSH "sudo add-apt-repository ppa:nginx/stable"
	$SSH "sudo apt-get update"
	$SSH "sudo apt-get install nginx"
}

function reload_nginx(){
	/etc/init.d/nginx restart
}


# Run deployment
function all(){
	upload_files
}
