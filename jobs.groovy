job("task6_job1"){
	scm {
		github('srishti-gupta20/DevOps-Task3-Repo', 'master')
	}
	steps {
		shell('''
			sudo cp -r . /host/devopstask3
		''')
	}
}

job("task6_job2"){
	
	triggers{
		upstream('task6_job1', 'SUCCESS')
	}
	steps{
shell(''' 
sudo chroot /host /bin/bash <<"EOT"
path="/devopstask3/.php"
filename=$(basename "$path")
extension="${filename##*.}"
sudo echo "extension of file: $extension"

if [ "$extension" == "php" ];
then
    if sudo  kubectl get pvc | egrep -w 'task3-web-pvc|task3-log-pvc'
    then
        echo "Both PVC already exist"
    else
        sudo kubectl create -f /devopstask3/web-deploy-pvc.yml
    fi

    if sudo kubectl get deployment | grep task3-web-deploy
    then
        sudo kubectl delete deployment task3-web-deploy
        sudo kubectl create -f /devopstask3/web-deploy.yml
        sleep 10
        podname=$(sudo kubectl get pods --no-headers -o custom-columns=":metadata.name" | grep task3-web-deploy)
        echo "$podname"
        sudo kubectl cp /devopstask3/index.php $podname:/var/www/html
    else
        sudo kubectl create -f /devopstask3/web-deploy.yml
        sleep 10
        podname=$(sudo kubectl get pods --no-headers -o custom-columns=":metadata.name" | grep task3-web-deploy)
        echo "$podname"
        sudo kubectl cp /devopstask3/index.php $podname:/var/www/html
    fi
    
    if sudo kubectl get svc | grep task3-service
    then
        echo "Service already exist"
    else
        sudo kubectl create -f /devopstask3/web-deploy-service.yml
    fi
    
else
    echo "Not a PHP file!!"
fi

echo "pod url: 192.168.99.100:31111"
EOT
''')
	}
}

job("task6_job3"){
	triggers{
		upstream('task6_job2', 'SUCCESS')
	}
	steps {
		shell('''
			export status=$(curl -o /dev/null -s -w "%{http_code}" http://192.168.99.100:31111/index.php)
			if [[ $status==200 ]]
			then
    				exit 0 
    				echo "Success" 
    				sudo python3 /devopstask3/success-mail.py
			else
    				exit 1 
    				echo "Failure"
    				sudo python3 /devopstask3/failure-mail.py
			fi
		''')
	}
}

buildPipelineView('DevOps AL Task-6 Build-pipeline') {
    filterBuildQueue()
    filterExecutors()
    title('DevOps AL task-6')
    displayedBuilds(1)
    selectedJob('task6_job1')
    refreshFrequency(3)
}
