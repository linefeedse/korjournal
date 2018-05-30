# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|

  config.vm.box = "regiohelden/ubuntu-16.04"
  config.vm.network :private_network, ip: "192.168.50.4"
  config.vm.network "forwarded_port", guest: 80, host: 8088
  config.vm.define "korjournal"

  # Optional (Remove if desired)
  #config.vm.provider :virtualbox do |v|
  #  v.customize ["modifyvm", :id, "--memory", "1024"]
  #  v.customize ["modifyvm", :id, "--cpus", "1"]
  #  v.customize ["modifyvm", :id, "--ioapic", "on"]
  #  v.customize ["modifyvm", :id, "--nictype1", "virtio"]
  #  v.name = "korjournal"
  #end
  config.vm.provider :libvirt do |v|
    v.memory = 1024
#    v.name = "korjournal"
  end
  config.vm.provision "ansible" do |ansible|
    ansible.playbook = "ansible/vagrant.yml"
    ansible.groups = {
        "group1" => [ "korjournal" ],
        "all_groups:children" => [ "group1" ]
    }
  end
  config.vm.provision "shell", path: "docker.sh"

  #config.vm.synced_folder ".", "/vagrant/", type: "nfs", mount_options: ['actimeo=1']
  config.vm.synced_folder './', '/vagrant', type: '9p', disabled: false, accessmode: "mapped"
end
