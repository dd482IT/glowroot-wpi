#!/bin/bash
BUILD_CMD="mvn compile -Pthe-checker-build"
CLEAN_CMD="mvn clean"
${BUILD_CMD} > .getSubProjectCount.txt 

WPITEMPDIR=/tmp/WPITEMP-glowroot
WPIOUTDIR=./build/whole-program-inference 

rm -rf ${WPITEMPDIR}
mkdir -p ${WPITEMPDIR}


count=1
subproject=1
subprojectTotal=52
while [[ "$subproject" != 52 ]] 
do
    if [[ ${DEBUG} == 1 ]]; then
	echo "entering iteration ${count}"
    fi
    ${CLEAN_CMD}
    # This mkdir is needed when the project has subprojects.
    mkdir -p "${WPITEMPDIR}"
    mkdir -p "${WPIOUTDIR}"
    DIFF_RESULT=$(diff -r ${WPITEMPDIR} ${WPIOUTDIR})
    if [[ ${DEBUG} == 1 ]]; then
	echo "putting the diff for iteration $count into $(realpath iteration$count.diff)"
	echo ${DIFF_RESULT} > iteration$count.diff
    fi
    while [[ "$DIFF_RESULT" != "" ]] || ((subprojectTotal++))
    do
        rm -rf ${WPITEMPDIR}
        mkdir -p "${WPITEMPDIR}"
        mv ${WPIOUTDIR} ${WPITEMPDIR}
        ${BUILD_CMD}
        ${CLEAN_CMD}
        SUB_DIFF_RESULT=$(diff -r ${WPITEMPDIR} ${WPIOUTDIR})
        echo ${SUB_DIFF_RESULT} > $SUB_DIFF_RESULT"-iteration-"$count.diff
        [[ "$SUB_DIFF_RESULT" != "" ]] || ((subprojectTotal++)) & break 1
    done
    ((count++))
done
