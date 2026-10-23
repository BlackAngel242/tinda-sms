import {test} from 'node:test';
import assert from 'node:assert/strict';
import {matches,recipients,personalize,deliverySummary} from '../src/domain.ts';
const rule={id:1,name:'Privé',contactId:2,keyword:'CONFIDENTIEL',when:'À la réception',action:'Coffre',delay:0,phrase:'test',enabled:true};
test('contact et mot-clé doivent tous deux correspondre',()=>{assert.equal(matches(rule,2,'Sujet confidentiel'),true);assert.equal(matches(rule,1,'confidentiel'),false);assert.equal(matches(rule,2,'Autre sujet'),false);assert.equal(matches({...rule,enabled:false},2,'confidentiel'),false);});
test('exclusions, doublons et conversations privées sont réévalués',()=>assert.deepEqual(recipients([1,2,2,3,4],[2],[4]),[1,3]));
test('variable absente signalée et message complet personnalisé',()=>{assert.deepEqual(personalize('Bonjour {prenom}, échéance {periode}',{prenom:'Aline'}),{text:'Bonjour Aline, échéance {periode}',missing:['periode']});assert.deepEqual(personalize('{prenom}',{prenom:'Aline'}),{text:'Aline',missing:[]});});
test('absence de confirmation et échec restent distincts',()=>assert.deepEqual(deliverySummary([{status:'Envoyé'},{status:'Livraison non confirmée'},{status:'Échec'}]),{'Envoyé':1,'Livraison non confirmée':1,'Échec':1}));
