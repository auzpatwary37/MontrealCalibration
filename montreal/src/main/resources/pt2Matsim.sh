cpus=40
time=24:00:00
mem='150G'

folder=/home/ashraf37/scratch/pt2Matsim
job_config=$folder/config.xml
output=/home/ashraf37/scratch/pt2Matsim



##############################################
echo "#!/bin/bash
#SBATCH --nodes=1
#SBATCH --cpus-per-task=$cpus
#SBATCH --ntasks=1
#SBATCH --time=$time
#SBATCH --mem=$mem
#SBATCH --job-name $pt2Matsim
#SBATCH --account=def-fciari
#SBATCH --mail-user=ashraf-uz-zaman.patwary@polymtl.ca
#SBATCH --mail-type=ALL

echo "Current working directory: `pwd`"
echo "Starting run at: `date`"

#Load environment
module load StdEnv/2021

# Load Java
module load java/17.0.2

# Load script
java -Xmx$mem -cp montreal-0.0.1-SNAPSHOT-jar-with-dependencies.jar network.NetworkWithLanesTrial $job_config

" > job_${output}.sh
###############################################

chmod +x job_${output}.sh

#srun -n 1 -c $cpus --mem=$mem --time=$time ./job_${output}.sh

sbatch ./job_${output}.sh

# ---------------------------------------------------------------------
echo "Job finished with exit code $? at: `date`"