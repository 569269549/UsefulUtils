# copy drawable icon 
# 1. convert from upper cased name to lower cased name.
# 2. hdpi to drawable-hdpi, xhdpi to drawable-xhdpi, etc
SRC="/home/duanwei/Desktop/android"
DST="/home/duanwei/AndroidStudioProjects/oga/app/src/main/res"

function checkDir() {
	if [[ ! -d $1 ]]; then
		echo "$1 is not valid dir!"
	fi
}

function myCopy() {
	srcDir=$1
	dstDir=$2
	echo "src $srcDir , dst $dstDir"
	checkDir $srcDir
	checkDir $dstDir

	for f in $(ls $srcDir)
	do
		local lowerCased=$(echo "$f" | tr '[:upper:]' '[:lower:]')
    echo "copy $srcDir/$f to $dstDir/$lowerCased"
    cp -f $srcDir/$f $dstDir/$lowerCased
	done
}

for subDir in $(ls $SRC)
do
  echo $subDir
  dstSubDir=""
  if [[ $subDir = "mdpi" ]] ; then
  	dstSubDir="drawable-mdpi"
  elif [[ $subDir = "hdpi" ]] ; then
  	dstSubDir="drawable-hdpi"
  elif [[ $subDir = "xhdpi" ]] ; then
  	dstSubDir="drawable-xhdpi"
  elif [[ $subDir = "xxhdpi" ]] ; then
  	dstSubDir="drawable-xxhdpi"
  elif [[ $subDir = "xxxhdpi" ]] ; then
  	dstSubDir="drawable-xxxhdpi"
  else 
  	echo "ignore $subDir"
    continue
  fi

  if [[ -z $dstSubDir ]]; then
  	break
  fi

  myCopy $SRC/$subDir $DST/$dstSubDir

done
