#!/bin/bash

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

DEFAULT_GROUP_ID="com.chief"
DEFAULT_ARTIFACT_ID="chief-boot-web"
DEFAULT_PACKAGE_NAME="com.chief"
DEFAULT_PROJECT_NAME="chief-boot-web"

usage() {
    echo ""
    echo -e "${CYAN}COLA Spring Boot Project Generator${NC}"
    echo ""
    echo -e "Usage: ${GREEN}$0${NC} [-n <project-name>] [-g <group-id>] [-a <artifact-id>] [-p <package-name>] [-o <output-dir>]"
    echo ""
    echo "Options:"
    echo "  -n    Project name (default: $DEFAULT_PROJECT_NAME)"
    echo "  -g    Group ID (default: $DEFAULT_GROUP_ID)"
    echo "  -a    Artifact ID (default: $DEFAULT_ARTIFACT_ID)"
    echo "  -p    Package name (default: $DEFAULT_PACKAGE_NAME)"
    echo "  -o    Output directory (default: current directory)"
    echo ""
    echo "Example:"
    echo -e "  ${GREEN}$0 -n order-service -g com.example -a order-service -p com.example.order -o /path/to/workspace${NC}"
    echo ""
    exit 1
}

PROJECT_NAME=""
GROUP_ID=""
ARTIFACT_ID=""
PACKAGE_NAME=""
OUTPUT_DIR="."

while getopts "n:g:a:p:o:h" opt; do
    case $opt in
        n) PROJECT_NAME="$OPTARG" ;;
        g) GROUP_ID="$OPTARG" ;;
        a) ARTIFACT_ID="$OPTARG" ;;
        p) PACKAGE_NAME="$OPTARG" ;;
        o) OUTPUT_DIR="$OPTARG" ;;
        h) usage ;;
        ?) usage ;;
    esac
done

if [ -z "$PROJECT_NAME" ]; then
    read -p "Project name [$DEFAULT_PROJECT_NAME]: " input
    PROJECT_NAME="${input:-$DEFAULT_PROJECT_NAME}"
fi

if [ -z "$GROUP_ID" ]; then
    read -p "Group ID [$DEFAULT_GROUP_ID]: " input
    GROUP_ID="${input:-$DEFAULT_GROUP_ID}"
fi

if [ -z "$ARTIFACT_ID" ]; then
    read -p "Artifact ID [$DEFAULT_ARTIFACT_ID]: " input
    ARTIFACT_ID="${input:-$DEFAULT_ARTIFACT_ID}"
fi

if [ -z "$PACKAGE_NAME" ]; then
    read -p "Package name [$DEFAULT_PACKAGE_NAME]: " input
    PACKAGE_NAME="${input:-$DEFAULT_PACKAGE_NAME}"
fi

if ! echo "$PROJECT_NAME" | grep -qE '^[a-z][a-z0-9]*(-[a-z0-9]+)*$'; then
    echo -e "${RED}Error: Project name must be lowercase, start with a letter, and use kebab-case (e.g., my-project)${NC}"
    exit 1
fi

if ! echo "$GROUP_ID" | grep -qE '^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)*$'; then
    echo -e "${RED}Error: Group ID must be a valid Java package (e.g., com.example)${NC}"
    exit 1
fi

if ! echo "$ARTIFACT_ID" | grep -qE '^[a-z][a-z0-9]*(-[a-z0-9]+)*$'; then
    echo -e "${RED}Error: Artifact ID must be lowercase, start with a letter, and use kebab-case${NC}"
    exit 1
fi

if ! echo "$PACKAGE_NAME" | grep -qE '^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)*$'; then
    echo -e "${RED}Error: Package name must be a valid Java package (e.g., com.example.myproject)${NC}"
    exit 1
fi

TEMPLATE_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="${OUTPUT_DIR}/${PROJECT_NAME}"
PACKAGE_PATH=$(echo "$PACKAGE_NAME" | tr '.' '/')

echo ""
echo -e "${CYAN}=========================================${NC}"
echo -e "${CYAN}  COLA Spring Boot Project Generator${NC}"
echo -e "${CYAN}=========================================${NC}"
echo -e "  Project Name:  ${GREEN}$PROJECT_NAME${NC}"
echo -e "  Artifact ID:   ${GREEN}$ARTIFACT_ID${NC}"
echo -e "  Group ID:      ${GREEN}$GROUP_ID${NC}"
echo -e "  Package Name:  ${GREEN}$PACKAGE_NAME${NC}"
echo -e "  Package Path:  ${GREEN}$PACKAGE_PATH${NC}"
echo -e "  Output Dir:    ${GREEN}$PROJECT_DIR${NC}"
echo -e "${CYAN}=========================================${NC}"
echo ""

if [ -d "$PROJECT_DIR" ]; then
    echo -e "${RED}Error: Directory $PROJECT_DIR already exists${NC}"
    exit 1
fi

echo -e "${YELLOW}[1/4]${NC} Copying template..."
mkdir -p "$OUTPUT_DIR"
cp -r "$TEMPLATE_DIR" "$PROJECT_DIR"
rm -f "$PROJECT_DIR/create.sh"
rm -f "$PROJECT_DIR/.gitignore"
rm -rf "$PROJECT_DIR/.git"

echo -e "${YELLOW}[2/4]${NC} Renaming module directories..."
for dir in "$PROJECT_DIR"/template-*; do
    if [ -d "$dir" ]; then
        base=$(basename "$dir")
        new_name="${base/template/$ARTIFACT_ID}"
        mv "$dir" "$(dirname "$dir")/$new_name"
        echo "       template-* -> $new_name"
    fi
done

echo -e "${YELLOW}[3/4]${NC} Setting up package directories..."
find "$PROJECT_DIR" -type d -name "__PACKAGE_PATH__" | while read -r pkg_dir; do
    parent_dir=$(dirname "$pkg_dir")
    target_dir="$parent_dir/$PACKAGE_PATH"
    mkdir -p "$target_dir"

    for item in "$pkg_dir"/*; do
        if [ -e "$item" ]; then
            mv "$item" "$target_dir/"
        fi
    done
    rm -rf "$pkg_dir"
    echo "       $pkg_dir -> $target_dir"
done

echo -e "${YELLOW}[4/4]${NC} Replacing placeholders in files..."
file_count=0
find "$PROJECT_DIR" -type f \( -name "*.java" -o -name "*.xml" -o -name "*.yml" -o -name "*.yaml" -o -name "*.properties" \) | while read -r file; do
    sed -i '' \
        -e "s|__GROUP_ID__|${GROUP_ID}|g" \
        -e "s|__ARTIFACT_ID__|${ARTIFACT_ID}|g" \
        -e "s|__PACKAGE_NAME__|${PACKAGE_NAME}|g" \
        -e "s|__PROJECT_NAME__|${PROJECT_NAME}|g" \
        "$file"
    file_count=$((file_count + 1))
done
echo "       Processed files with placeholder replacements"

echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}  Project created successfully!${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo -e "  Location: ${CYAN}$PROJECT_DIR${NC}"
echo ""
echo "  Next steps:"
echo -e "    ${CYAN}cd $PROJECT_DIR${NC}"
echo -e "    ${CYAN}mvn clean install -DskipTests${NC}"
echo -e "    ${CYAN}cd ${ARTIFACT_ID}-start${NC}"
echo -e "    ${CYAN}mvn spring-boot:run${NC}"
echo ""
echo "  Code Generator:"
echo -e "    ${CYAN}cd ${ARTIFACT_ID}-generator${NC}"
echo -e "    ${CYAN}mvn compile exec:java -Dexec.mainClass=\"${PACKAGE_NAME}.generator.CodeGenerator\"${NC}"
echo ""
