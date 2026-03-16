#!/bin/bash
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

CLUSTER_NAME="order-app-cluster"
NODEGROUP_NAME="my-nodegroup"
INSTANCE_TYPE="t3.small"
NODE_COUNT=2
RDS_IDENTIFIER="order-app-db"
ECS_CLUSTER="spring-api-cluster"
ECS_SERVICE="spring-api-service"
REGION="us-east-1"

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}  START - RDS + ECS + EKS               ${NC}"
echo -e "${BLUE}=========================================${NC}"

# ---- 1. Iniciar RDS ----
echo -e "\n${BLUE}[1/4] A iniciar RDS...${NC}"
RDS_STATUS=$(aws rds describe-db-instances \
  --db-instance-identifier $RDS_IDENTIFIER \
  --query 'DBInstances[0].DBInstanceStatus' \
  --output text 2>/dev/null)

# ✅ Aguardar caso ainda esteja a parar
if [ "$RDS_STATUS" == "stopping" ]; then
  echo "  RDS ainda a parar... a aguardar terminar..."
  while [ "$RDS_STATUS" != "stopped" ]; do
    sleep 15
    RDS_STATUS=$(aws rds describe-db-instances \
      --db-instance-identifier $RDS_IDENTIFIER \
      --query 'DBInstances[0].DBInstanceStatus' \
      --output text 2>/dev/null)
    echo "  RDS status: $RDS_STATUS"
  done
fi

if [ "$RDS_STATUS" == "available" ]; then
  echo -e "${GREEN}✓ RDS já está disponível${NC}"
elif [ "$RDS_STATUS" == "stopped" ]; then
  aws rds start-db-instance --db-instance-identifier $RDS_IDENTIFIER --region $REGION > /dev/null
  echo -e "${GREEN}✓ RDS a iniciar... a aguardar ficar disponível...${NC}"
  while true; do
    STATUS=$(aws rds describe-db-instances \
      --db-instance-identifier $RDS_IDENTIFIER \
      --query 'DBInstances[0].DBInstanceStatus' \
      --output text 2>/dev/null)
    echo "  RDS status: $STATUS"
    [ "$STATUS" == "available" ] && echo -e "${GREEN}✓ RDS disponível!${NC}" && break
    sleep 15
  done
else
  echo "  RDS status: $RDS_STATUS (inesperado)"
fi

# ---- 2. Iniciar ECS ----
echo -e "\n${BLUE}[2/4] A iniciar ECS...${NC}"
ECS_EXISTS=$(aws ecs describe-services \
  --cluster $ECS_CLUSTER --services $ECS_SERVICE \
  --region $REGION \
  --query 'services[0].status' --output text 2>/dev/null)

if [ "$ECS_EXISTS" == "ACTIVE" ]; then
  aws ecs update-service --cluster $ECS_CLUSTER --service $ECS_SERVICE \
    --desired-count 1 --region $REGION > /dev/null
  echo -e "${GREEN}✓ ECS service escalado para 1 task${NC}"

  echo "  A aguardar ECS task ficar RUNNING..."
  for i in {1..20}; do
    RUNNING=$(aws ecs describe-services \
      --cluster $ECS_CLUSTER --services $ECS_SERVICE \
      --region $REGION \
      --query 'services[0].runningCount' --output text 2>/dev/null)
    if [ "$RUNNING" == "1" ]; then
      echo -e "${GREEN}✓ ECS task RUNNING${NC}"
      break
    fi
    echo "  ECS running tasks: $RUNNING/1... ($i/20)"
    sleep 15
  done
else
  echo "  ECS service não encontrado"
fi

# ---- 3. Recriar Cluster EKS ----
echo -e "\n${BLUE}[3/4] A recriar cluster EKS (15-20 min)...${NC}"
CLUSTER_EXISTS=$(aws eks describe-cluster --name $CLUSTER_NAME --region $REGION \
  --query 'cluster.status' --output text 2>/dev/null)

if [ "$CLUSTER_EXISTS" == "ACTIVE" ]; then
  echo -e "${GREEN}✓ Cluster já existe${NC}"
else
  eksctl create cluster \
    --name $CLUSTER_NAME \
    --region $REGION \
    --nodegroup-name $NODEGROUP_NAME \
    --node-type $INSTANCE_TYPE \
    --nodes $NODE_COUNT \
    --nodes-min 1 \
    --nodes-max 2 \
    --managed
  echo -e "${GREEN}✓ Cluster EKS criado${NC}"
fi

# ---- 4. Configurar kubectl e aplicar deployments ----
echo -e "\n${BLUE}[4/4] A configurar kubectl e aplicar deployments...${NC}"
aws eks update-kubeconfig --name $CLUSTER_NAME --region $REGION > /dev/null 2>&1
echo -e "${GREEN}✓ kubectl configurado${NC}"

echo "  A aguardar nodes ficarem Ready..."
kubectl wait --for=condition=Ready nodes --all --timeout=300s
echo -e "${GREEN}✓ Nodes prontos${NC}"

echo "  A autenticar no ECR..."
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
kubectl create secret docker-registry ecr-secret \
  --docker-server=$AWS_ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com \
  --docker-username=AWS \
  --docker-password=$(aws ecr get-login-password --region $REGION) \
  --dry-run=client -o yaml | kubectl apply -f - > /dev/null
echo -e "${GREEN}✓ ECR secret aplicado${NC}"

if [ -f "kubernate/deployment.yaml" ]; then
  kubectl apply -f kubernate/deployment.yaml > /dev/null
  kubectl apply -f kubernate/service.yaml > /dev/null 2>&1 || true
  echo -e "${GREEN}✓ Deployments aplicados${NC}"
else
  echo "  (sem deployment.yaml, a ignorar)"
fi

# ✅ Restart garante pods arrancam com RDS já disponível
echo "  A reiniciar pods..."
kubectl rollout restart deployment/spring-api-deployment > /dev/null 2>&1 || true
kubectl rollout status deployment/spring-api-deployment --timeout=120s 2>/dev/null || true
echo -e "${GREEN}✓ Pods reiniciados${NC}"

# ---- Resultado ----
echo -e "\n${BLUE}=========================================${NC}"
echo -e "${GREEN}  INFRA INICIADA!                       ${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""
kubectl get nodes 2>/dev/null
echo ""
kubectl get pods 2>/dev/null
echo ""
echo -e "${BLUE}RDS Endpoint:${NC}"
aws rds describe-db-instances \
  --db-instance-identifier $RDS_IDENTIFIER \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text 2>/dev/null
echo ""
echo -e "${BLUE}Services Kubernetes:${NC}"
kubectl get svc 2>/dev/null
echo ""

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}  URL para o API Gateway:               ${NC}"
echo -e "${BLUE}=========================================${NC}"
LB_URL=$(kubectl get svc spring-api-service \
  --output jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null)
if [ -n "$LB_URL" ]; then
  echo -e "${GREEN}  http://$LB_URL/{proxy}${NC}"
  echo ""
  echo -e "  👉 Vai ao API Gateway → Editar integração → cola este URL"
else
  echo -e "  A aguardar Load Balancer... tenta em 1 min: kubectl get svc"
fi
echo ""
