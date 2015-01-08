#!/bin/bash
	
BASEDIR=$(dirname $(readlink -f $0))
if [ ! -n "$prefix" ]; then
	echo "Environment variable \"\$prefix\" must be set!"
	exit 1
fi
cd ${BASEDIR}
echo "Re-compiling:"
echo "    FrameTransform.proto"
echo ""
protoc \
--proto_path=src/main/resources/proto:$prefix/share/rst0.11/proto/stable/:$prefix/share/rst0.11/proto/sandbox/ \
--java_out=src/main/java/ \
src/main/resources/proto/FrameTransform.proto \

echo "done."
